package com.xbk.knowledge.infrastructure.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.app.GatewayObservabilityAppService;
import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolSchema;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolMappingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolNameQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolMappingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolRegistryRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolSchemaRepository;
import com.xbk.knowledge.domain.gateway.service.GatewayToolService;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Gateway 工具域服务实现。
 * <p>
 * 职责：实现 Gateway 工具的核心业务逻辑，包括
 * 1. 工具清单查询（listTools）加载已启用工具并生成 JSON Schema
 * 2. 工具调用执行（callTool）参数映射 → HTTP 请求 → 响应提取 → 指标记录
 * 3. 协议初始化（initialize）返回网关能力声明
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayToolServiceImpl implements GatewayToolService {

    /**
     * 工具调用链路追踪 ID 的 MDC 键。
     */
    private static final String CALL_ID_MDC_KEY = "gatewayToolCallId";

    /**
     * MCP 协议版本号。
     */
    private static final String PROTOCOL_VERSION = "2024-11-05";

    /**
     * 参数映射类型请求方向。
     */
    private static final String REQUEST_MAPPING_TYPE = "request";

    /**
     * 参数映射类型响应方向。
     */
    private static final String RESPONSE_MAPPING_TYPE = "response";

    /**
     * 默认请求超时时间（毫秒）。
     */
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    /**
     * 默认重试次数。
     */
    private static final int DEFAULT_RETRY_TIMES = 0;

    /**
     * 网关仓储。
     */
    private final McpGatewayRepository gatewayRepository;

    /**
     * 工具注册表仓储。
     */
    private final McpToolRegistryRepository toolRegistryRepository;

    /**
     * 工具映射仓储。
     */
    private final McpToolMappingRepository toolMappingRepository;

    /**
     * 工具 Schema 仓储。
     */
    private final McpToolSchemaRepository toolSchemaRepository;

    /**
     * 网关可观测性应用服务。
     */
    private final GatewayObservabilityAppService gatewayObservabilityAppService;

    /**
     * JSON 序列化/反序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * WebClient 构建器。
     */
    private final WebClient.Builder webClientBuilder;

    /**
     * WebClient 实例。
     */
    private WebClient webClient;

    @PostConstruct
    public void initWebClient() {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 查询网关下所有已启用工具的定义列表
     * 每个工具包含 name、description 和根据参数映射生成的 inputSchema
     *
     * @param gatewayId 标识 ID。
     * @return ToolDefinition 列表。
     */
    @Override
    public List<ToolDefinition> listTools(String gatewayId) {
        // 先校验网关存在且启用，后续查询统一使用规范化后的 gatewayId。
        McpGateway gateway = requireEnabledGateway(gatewayId);
        // 仅返回该网关下“已启用”的工具，屏蔽草稿/禁用状态工具。
        GatewayIdQuery gatewayIdQuery = new GatewayIdQuery(gateway.getGatewayId());
        List<McpToolRegistry> tools = toolRegistryRepository.findEnabledByGatewayId(gatewayIdQuery);
        if (tools == null || tools.isEmpty()) {
            // 对上层保持稳定契约：无数据返回空集合，不返回 null。
            return Collections.emptyList();
        }

        List<ToolDefinition> definitions = new ArrayList<>();
        for (McpToolRegistry tool : tools) {
            if (tool == null || tool.getId() == null) {
                continue;
            }
            // 读取请求参数映射，并据此组装 MCP 工具 inputSchema。
            List<McpToolMapping> requestMappings = toolMappingRepository.findByToolIdAndMappingType(
                    new ToolMappingQuery(tool.getId(), REQUEST_MAPPING_TYPE)
            );
            // 解析 inputSchema
            Map<String, Object> inputSchema = resolveInputSchema(gatewayId, tool, requestMappings);
            // 描述为空时降级为空串，避免上层消费时出现空值分支。
            definitions.add(new ToolDefinition(
                    tool.getToolName(),
                    StringUtils.hasText(tool.getToolDescription()) ? tool.getToolDescription() : "",
                    inputSchema
            ));
        }
        return definitions;
    }

    /**
     * 执行工具调用
     * 流程：校验工具 → 加载参数映射 → 构建 HTTP 请求 → 带重试执行 → 提取响应 → 记录指标
     *
     * @param gatewayId 标识 ID。
     * @param toolName  工具名称。
     * @param arguments 工具调用参数。
     * @return 工具调用结果。
     */
    @Override
    public ToolCallResult callTool(String gatewayId, String toolName, Map<String, Object> arguments) {
        long startAt = System.nanoTime();
        String callId = resolveCallId();
        Map<String, Object> safeArguments = arguments == null ? Collections.emptyMap() : arguments;
        log.info("gateway_tool_call source=SERVICE stage=start callId={} gatewayId={} toolName={} argsKeys={}",
                callId,
                gatewayId,
                toolName,
                safeArguments.keySet());

        if (!StringUtils.hasText(gatewayId) || !StringUtils.hasText(toolName)) {
            return buildFailureResult(callId, gatewayId, toolName, "工具调用参数不完整", "INVALID_PARAMS", null, startAt, safeArguments);
        }

        Optional<McpToolRegistry> toolOptional = toolRegistryRepository.findByGatewayIdAndToolName(
                new ToolNameQuery(gatewayId, toolName)
        );
        if (toolOptional.isEmpty()) {
            return buildFailureResult(callId, gatewayId, toolName, "工具不存在: " + toolName, "TOOL_NOT_FOUND", null, startAt, safeArguments);
        }

        McpToolRegistry tool = toolOptional.get();
        if (tool.getStatus() == null || tool.getStatus() != 1) {
            return buildFailureResult(callId, gatewayId, toolName, "工具未启用: " + toolName, "TOOL_DISABLED", tool.getTimeout(), startAt, safeArguments);
        }

        List<McpToolMapping> requestMappings = toolMappingRepository.findByToolIdAndMappingType(
                new ToolMappingQuery(tool.getId(), REQUEST_MAPPING_TYPE)
        );
        List<McpToolMapping> responseMappings = toolMappingRepository.findByToolIdAndMappingType(
                new ToolMappingQuery(tool.getId(), RESPONSE_MAPPING_TYPE)
        );

        try {
            HttpInvokePayload payload = buildInvokePayload(tool, requestMappings, safeArguments);
            String responseText = executeWithRetry(payload, tool.getRetryTimes(), tool.getTimeout());
            String finalResult = extractResponse(responseText, responseMappings);
            return buildSuccessResult(callId, gatewayId, toolName, finalResult, tool.getTimeout(), startAt, safeArguments);
        } catch (Exception e) {
            log.error("Gateway 工具调用失败 callId: {}, gatewayId: {}, toolName: {}", callId, gatewayId, toolName, e);
            String errorCode = classifyErrorCode(e);
            return buildFailureResult(callId, gatewayId, toolName, e.getMessage(), errorCode, tool.getTimeout(), startAt, safeArguments);
        }
    }

    /**
     * 处理 MCP initialize 握手，返回网关能力声明
     *
     * @param gatewayId 标识 ID。
     * @return 网关信息。
     */
    @Override
    public GatewayInfo initialize(String gatewayId) {
        McpGateway gateway = requireEnabledGateway(gatewayId);
        String serverName = StringUtils.hasText(gateway.getGatewayName()) ? gateway.getGatewayName() : gateway.getGatewayId();
        String serverVersion = StringUtils.hasText(gateway.getGatewayVersion()) ? gateway.getGatewayVersion() : "1.0.0";
        String instructions = StringUtils.hasText(gateway.getGatewayInstructions()) ? gateway.getGatewayInstructions() : "";
        return new GatewayInfo(PROTOCOL_VERSION, serverName, serverVersion, instructions);
    }

    /**
     * 校验网关存在且已启用，否则抛出 BusinessException
     *
     * @param gatewayId 标识 ID。
     * @return 网关配置。
     */
    private McpGateway requireEnabledGateway(String gatewayId) {
        // 先做入参校验，避免无效 gatewayId 继续进入仓储查询。
        if (!StringUtils.hasText(gatewayId)) {
            throw new BusinessException("gatewayId 不能为空");
        }
        // 按业务唯一键加载网关，未命中按“资源不存在”处理。
        GatewayIdQuery gatewayIdQuery = new GatewayIdQuery(gatewayId);
        // 查询 MCP 网关
        Optional<McpGateway> gatewayOptional = gatewayRepository.findByGatewayId(gatewayIdQuery);
        if (gatewayOptional.isEmpty()) {
            throw new NotFoundException("网关不存在: " + gatewayId);
        }
        McpGateway gateway = gatewayOptional.get();
        // 状态约束：仅允许启用态网关参与工具发现与调用。
        if (gateway.getStatus() == null || gateway.getStatus() != 1) {
            throw new BusinessException("网关未启用: " + gatewayId);
        }
        return gateway;
    }

    /**
     * 解析工具的 inputSchema
     * 优先从 Schema 缓存中读取（通过 hash 比对判断是否过期），缓存未命中时重新生成并持久化
     *
     * @param gatewayId 标识 ID。
     * @param tool      工具注册配置。
     * @param mappings  工具参数映射列表。
     */
    private Map<String, Object> resolveInputSchema(String gatewayId,
                                                   McpToolRegistry tool,
                                                   List<McpToolMapping> mappings) {
        // 以映射配置计算哈希，作为 schema 缓存是否可复用的版本标识。
        String mappingHash = computeMappingHash(mappings);
        // 读取当前生效的 schema 缓存记录（若存在）。
        Optional<McpToolSchema> cachedOptional = toolSchemaRepository.findActiveByGatewayIdAndToolId(gatewayId, tool.getId());
        if (cachedOptional.isPresent()) {
            McpToolSchema cached = cachedOptional.get();
            // 仅当缓存内容完整且哈希一致时判定为命中，避免使用过期或脏数据。
            if (StringUtils.hasText(cached.getInputSchema())
                    && StringUtils.hasText(cached.getSchemaHash())
                    && cached.getSchemaHash().equals(mappingHash)) {
                Map<String, Object> cachedSchema = parseJsonMap(cached.getInputSchema());
                // 反序列化结果非空才返回，空对象视为无效缓存并走重建流程。
                if (!cachedSchema.isEmpty()) {
                    return cachedSchema;
                }
            }
        }

        // 缓存未命中或已失效：基于最新映射重建 schema，并回写缓存。
        Map<String, Object> schema = buildInputSchema(mappings);
        saveInputSchemaCache(gatewayId, tool.getId(), mappingHash, schema, cachedOptional.orElse(null));
        return schema;
    }

    /**
     * 持久化 inputSchema 缓存（新增或更新版本号）
     *
     * @param gatewayId   标识 ID。
     * @param toolId      标识 ID。
     * @param mappingHash 映射哈希。
     * @param schema      工具 schema 映射。
     * @param oldSchema   历史 schema 记录。
     */
    private void saveInputSchemaCache(String gatewayId,
                                      Long toolId,
                                      String mappingHash,
                                      Map<String, Object> schema,
                                      McpToolSchema oldSchema) {
        McpToolSchema schemaEntity = oldSchema == null ? new McpToolSchema() : oldSchema;
        schemaEntity.setGatewayId(gatewayId);
        schemaEntity.setToolId(toolId);
        schemaEntity.setSchemaVersion(resolveNextSchemaVersion(oldSchema));
        schemaEntity.setInputSchema(toJson(schema));
        schemaEntity.setSchemaHash(mappingHash);
        schemaEntity.setIsActive(Boolean.TRUE);
        schemaEntity.setUpdatedAt(LocalDateTime.now());
        if (schemaEntity.getCreatedAt() == null) {
            schemaEntity.setCreatedAt(LocalDateTime.now());
        }
        toolSchemaRepository.save(schemaEntity);
    }

    /**
     * 计算 schema 下一版本号。
     *
     * @param oldSchema 历史 schema 记录。
     * @return 下一版本号（无历史版本时返回 1）。
     */
    private int resolveNextSchemaVersion(McpToolSchema oldSchema) {
        if (oldSchema == null || oldSchema.getSchemaVersion() == null) {
            return 1;
        }
        return oldSchema.getSchemaVersion() + 1;
    }

    /**
     * 根据参数映射树形结构生成 JSON Schema（支持嵌套 object/array）
     *
     * @param mappings 参数映射列表。
     */
    private Map<String, Object> buildInputSchema(List<McpToolMapping> mappings) {
        // 无映射时返回一个空 object schema，保持上层协议结构稳定。
        if (mappings == null || mappings.isEmpty()) {
            return buildObjectSchema(Collections.emptyMap(), Collections.emptyList());
        }

        // 第一阶段：建立节点索引，供后续父子关系挂接时快速查找父节点。
        Map<Long, McpToolMapping> nodeMap = buildNodeIndex(mappings);
        // 第二阶段：根据 parentId 构建父子关系，并收集根节点。
        Map<Long, List<McpToolMapping>> childrenMap = new HashMap<>();
        List<McpToolMapping> roots = collectRootNodes(mappings, nodeMap, childrenMap);

        // 根节点按排序号稳定排序，保证 schema 输出顺序可预期。
        roots.sort(Comparator.comparingInt(this::safeSortOrder));
        // 兼容特殊结构：仅存在一个 arguments 根节点时，直接展开其子节点作为顶层对象属性。
        if (isSingleArgumentsRoot(roots)) {
            return buildObjectSchemaFromChildren(roots.get(0).getId(), childrenMap, nodeMap, new HashSet<>());
        }
        // 常规结构：以根节点集合构建顶层 object schema。
        return buildObjectSchemaFromNodes(roots, childrenMap, nodeMap, new HashSet<>());
    }

    /**
     * 建立节点索引（id -> 节点），过滤空节点与无 id 节点。
     *
     * @param mappings 参数映射列表。
     * @return 节点索引。
     */
    private Map<Long, McpToolMapping> buildNodeIndex(List<McpToolMapping> mappings) {
        Map<Long, McpToolMapping> nodeMap = new HashMap<>();
        for (McpToolMapping mapping : mappings) {
            if (mapping == null || mapping.getId() == null) {
                continue;
            }
            nodeMap.put(mapping.getId(), mapping);
        }
        return nodeMap;
    }

    /**
     * 根据 parentId 构建父子关系，并返回根节点集合。
     *
     * @param mappings    参数映射列表。
     * @param nodeMap     节点索引。
     * @param childrenMap 子节点映射（输出参数）。
     * @return 根节点列表。
     */
    private List<McpToolMapping> collectRootNodes(List<McpToolMapping> mappings,
                                                  Map<Long, McpToolMapping> nodeMap,
                                                  Map<Long, List<McpToolMapping>> childrenMap) {
        List<McpToolMapping> roots = new ArrayList<>();
        for (McpToolMapping mapping : mappings) {
            // 过滤脏数据：无节点主键的映射无法参与树结构构建。
            if (mapping == null || mapping.getId() == null) {
                continue;
            }
            Long parentId = mapping.getParentId();
            // parentId 为空或父节点不存在时，按根节点处理（兼容不完整树数据）。
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(mapping);
                continue;
            }
            // 构建 parentId -> children 列表，供后续递归生成 schema。
            childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(mapping);
        }
        return roots;
    }

    /**
     * 是否仅存在一个 arguments 根节点。
     *
     * @param roots 根节点列表。
     * @return true 表示可直接展开 arguments 的子节点。
     */
    private boolean isSingleArgumentsRoot(List<McpToolMapping> roots) {
        return roots.size() == 1 && "arguments".equalsIgnoreCase(roots.get(0).getFieldName());
    }

    /**
     * 根据子节点关系构建对象 Schema。
     *
     * @param parentId    父节点ID。
     * @param childrenMap 子节点映射。
     * @param nodeMap     节点索引映射。
     * @param visiting    递归访问链路。
     */
    private Map<String, Object> buildObjectSchemaFromChildren(Long parentId,
                                                              Map<Long, List<McpToolMapping>> childrenMap,
                                                              Map<Long, McpToolMapping> nodeMap,
                                                              Set<Long> visiting) {
        List<McpToolMapping> children = childrenMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return buildObjectSchema(Collections.emptyMap(), Collections.emptyList());
        }
        children.sort(Comparator.comparingInt(this::safeSortOrder));
        return buildObjectSchemaFromNodes(children, childrenMap, nodeMap, visiting);
    }

    /**
     * 根据节点集合构建对象 Schema。
     *
     * @param nodes       节点列表。
     * @param childrenMap 子节点映射。
     * @param nodeMap     节点索引映射。
     * @param visiting    递归访问链路。
     */
    private Map<String, Object> buildObjectSchemaFromNodes(List<McpToolMapping> nodes,
                                                           Map<Long, List<McpToolMapping>> childrenMap,
                                                           Map<Long, McpToolMapping> nodeMap,
                                                           Set<Long> visiting) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (McpToolMapping node : nodes) {
            if (!StringUtils.hasText(node.getFieldName())) {
                continue;
            }
            properties.put(node.getFieldName(), buildNodeSchema(node, childrenMap, nodeMap, visiting));
            if (Boolean.TRUE.equals(node.getIsRequired())) {
                required.add(node.getFieldName());
            }
        }
        return buildObjectSchema(properties, required);
    }

    /**
     * 构建节点 Schema。
     *
     * @param node        节点定义。
     * @param childrenMap 子节点映射。
     * @param nodeMap     节点索引映射。
     * @param visiting    递归访问链路。
     */
    private Map<String, Object> buildNodeSchema(McpToolMapping node,
                                                Map<Long, List<McpToolMapping>> childrenMap,
                                                Map<Long, McpToolMapping> nodeMap,
                                                Set<Long> visiting) {
        if (node == null || node.getId() == null) {
            return buildObjectSchema(Collections.emptyMap(), Collections.emptyList());
        }
        if (!visiting.add(node.getId())) {
            log.warn("参数映射存在循环引用，nodeId: {}", node.getId());
            return buildObjectSchema(Collections.emptyMap(), Collections.emptyList());
        }

        try {
            String type = normalizeSchemaType(node.getMcpType());
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", type);
            if (StringUtils.hasText(node.getMcpDesc())) {
                schema.put("description", node.getMcpDesc());
            }

            if ("object".equals(type)) {
                List<McpToolMapping> children = childrenMap.get(node.getId());
                if (children != null && !children.isEmpty()) {
                    children.sort(Comparator.comparingInt(this::safeSortOrder));
                    Map<String, Object> objectSchema = buildObjectSchemaFromNodes(children, childrenMap, nodeMap, visiting);
                    schema.putAll(objectSchema);
                }
            }

            if ("array".equals(type)) {
                schema.put("items", buildArrayItems(node, childrenMap, nodeMap, visiting));
            }

            return schema;
        } finally {
            visiting.remove(node.getId());
        }
    }

    private Map<String, Object> buildArrayItems(McpToolMapping node,
                                                Map<Long, List<McpToolMapping>> childrenMap,
                                                Map<Long, McpToolMapping> nodeMap,
                                                Set<Long> visiting) {
        if (node.getItemRefId() != null && nodeMap.containsKey(node.getItemRefId())) {
            return buildNodeSchema(nodeMap.get(node.getItemRefId()), childrenMap, nodeMap, visiting);
        }

        if (StringUtils.hasText(node.getItemType())) {
            Map<String, Object> items = new LinkedHashMap<>();
            items.put("type", normalizeSchemaType(node.getItemType()));
            return items;
        }

        List<McpToolMapping> children = childrenMap.get(node.getId());
        if (children != null && !children.isEmpty()) {
            children.sort(Comparator.comparingInt(this::safeSortOrder));
            return buildObjectSchemaFromNodes(children, childrenMap, nodeMap, visiting);
        }

        Map<String, Object> items = new LinkedHashMap<>();
        items.put("type", "string");
        return items;
    }

    /**
     * 构建 HTTP 调用载荷
     * 根据参数映射规则将 MCP arguments 分发到 header/query/path/body 各位置
     *
     * @param tool            工具注册配置。
     * @param requestMappings 请求参数映射列表。
     * @param arguments       工具调用参数。
     * @return HTTP 调用载荷。
     */
    private HttpInvokePayload buildInvokePayload(McpToolRegistry tool,
                                                 List<McpToolMapping> requestMappings,
                                                 Map<String, Object> arguments) {
        HttpInvokePayload payload = new HttpInvokePayload();
        payload.url = tool.getHttpUrl();
        payload.method = resolveHttpMethod(tool.getHttpMethod());
        payload.headers = parseHeaders(tool.getHttpHeaders());
        payload.query = new LinkedHashMap<>();
        payload.body = new LinkedHashMap<>();

        if (requestMappings == null || requestMappings.isEmpty()) {
            if (supportsBody(payload.method)) {
                payload.body.putAll(arguments);
            } else {
                payload.query.putAll(arguments);
            }
            return payload;
        }

        Map<Long, McpToolMapping> nodeMap = new HashMap<>();
        for (McpToolMapping mapping : requestMappings) {
            if (mapping != null && mapping.getId() != null) {
                nodeMap.put(mapping.getId(), mapping);
            }
        }

        for (McpToolMapping mapping : requestMappings) {
            if (mapping == null || !StringUtils.hasText(mapping.getHttpPath()) || !StringUtils.hasText(mapping.getHttpLocation())) {
                continue;
            }

            String sourcePath = resolveSourcePath(mapping, nodeMap);
            Object value = readValueByPath(arguments, sourcePath);
            if (value == null && StringUtils.hasText(mapping.getFieldName())) {
                value = arguments.get(mapping.getFieldName());
            }
            if (value == null) {
                continue;
            }
            applyValueToPayload(payload, mapping.getHttpLocation(), mapping.getHttpPath(), value);
        }

        return payload;
    }

    /**
     * 将参数值写入 HTTP 载荷的指定位置（header/query/path/body）
     *
     * @param payload      HTTP 调用载荷。
     * @param httpLocation String 参数。
     * @param httpPath     路径。
     * @param value        参数值。
     */
    private void applyValueToPayload(HttpInvokePayload payload,
                                     String httpLocation,
                                     String httpPath,
                                     Object value) {
        String location = httpLocation.toLowerCase(Locale.ROOT);
        if ("header".equals(location)) {
            payload.headers.put(httpPath, String.valueOf(value));
            return;
        }

        if ("query".equals(location)) {
            payload.query.put(httpPath, value);
            return;
        }

        if ("path".equals(location)) {
            payload.url = replacePathVariable(payload.url, httpPath, value);
            return;
        }

        setPathValue(payload.body, httpPath, value);
    }

    /**
     * 带重试的 HTTP 调用执行
     *
     * @param payload    HTTP 调用载荷。
     * @param retryTimes 重试次数。
     * @param timeout    超时时间。
     * @return HTTP 响应文本。
     */
    private String executeWithRetry(HttpInvokePayload payload, Integer retryTimes, Integer timeout) {
        int attempts = normalizeRetryTimes(retryTimes) + 1;
        int timeoutMs = normalizeTimeout(timeout);
        Exception lastException = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                return executeOnce(payload, timeoutMs);
            } catch (Exception e) {
                lastException = e;
                if (i < attempts) {
                    log.warn("HTTP 工具调用失败，准备重试 {}/{}，url: {}，原因: {}", i, attempts, payload.url, e.getMessage());
                }
            }
        }
        throw new IllegalStateException("HTTP 工具调用失败", lastException);
    }

    /**
     * 执行单次 HTTP 请求（WebClient 同步阻塞）
     *
     * @param payload   HTTP 调用载荷。
     * @param timeoutMs 超时时间（毫秒）。
     * @return HTTP 响应文本。
     */
    private String executeOnce(HttpInvokePayload payload, int timeoutMs) {
        String finalUrl = buildFinalUrl(payload.url, payload.query);
        WebClient.RequestBodySpec request = webClient
                .method(payload.method)
                .uri(finalUrl)
                .header("X-Trace-Id", TraceIdUtils.getOrCreateTraceId())
                .headers(headers -> applyHeaders(headers, payload.headers));

        WebClient.RequestHeadersSpec<?> requestHeadersSpec;
        if (supportsBody(payload.method)) {
            requestHeadersSpec = request
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload.body);
        } else {
            requestHeadersSpec = request;
        }

        return requestHeadersSpec
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new IllegalStateException(
                                        "HTTP 调用失败，status=" + response.statusCode().value() + ", body=" + body
                                )))
                )
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
    }

    /**
     * 根据响应映射规则从原始响应中提取指定字段
     *
     * @param rawResponse      原始响应。
     * @param responseMappings 响应字段映射列表。
     * @return 处理后的响应文本。
     */
    private String extractResponse(String rawResponse,
                                   List<McpToolMapping> responseMappings) {
        if (responseMappings == null || responseMappings.isEmpty()) {
            return rawResponse;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawResponse);
        } catch (Exception e) {
            log.warn("响应非 JSON，降级原样返回。原因: {}", e.getMessage());
            return rawResponse;
        }

        Map<String, Object> extracted = new LinkedHashMap<>();
        boolean hasSuccess = false;

        for (McpToolMapping mapping : responseMappings) {
            if (mapping == null || !StringUtils.hasText(mapping.getHttpPath())) {
                continue;
            }
            try {
                Object value = extractByPathExpression(root, mapping.getHttpPath());
                if (value == null) {
                    continue;
                }
                String fieldName = StringUtils.hasText(mapping.getFieldName())
                        ? mapping.getFieldName()
                        : mapping.getHttpPath();
                extracted.put(fieldName, value);
                hasSuccess = true;
            } catch (Exception e) {
                log.warn("响应提取失败 path: {}，error: {}", mapping.getHttpPath(), e.getMessage());
            }
        }

        if (!hasSuccess) {
            return rawResponse;
        }
        return toJson(extracted);
    }

    /**
     * 按路径表达式从 JSON 树中提取值（支持嵌套路径和数组下标 [n]/[*]）
     *
     * @param root       JSON 根节点。
     * @param expression 表达式。
     * @return 表达式解析结果。
     */
    private Object extractByPathExpression(JsonNode root, String expression) {
        if (root == null || !StringUtils.hasText(expression)) {
            return null;
        }
        List<String> tokens = splitPathTokens(expression);
        List<JsonNode> current = new ArrayList<>();
        current.add(root);

        for (String token : tokens) {
            List<JsonNode> next = new ArrayList<>();
            String fieldName = token;
            String indexExpr = null;
            int idxStart = token.indexOf('[');
            if (idxStart >= 0 && token.endsWith("]")) {
                fieldName = token.substring(0, idxStart);
                indexExpr = token.substring(idxStart + 1, token.length() - 1);
            }

            for (JsonNode node : current) {
                JsonNode target = node;
                if (StringUtils.hasText(fieldName)) {
                    target = target.get(fieldName);
                }
                if (target == null || target.isMissingNode() || target.isNull()) {
                    continue;
                }

                if (indexExpr == null) {
                    next.add(target);
                    continue;
                }

                if ("*".equals(indexExpr)) {
                    if (target.isArray()) {
                        target.forEach(next::add);
                    }
                    continue;
                }

                int index = Integer.parseInt(indexExpr);
                if (target.isArray() && index >= 0 && index < target.size()) {
                    next.add(target.get(index));
                }
            }

            current = next;
            if (current.isEmpty()) {
                return null;
            }
        }

        if (current.size() == 1) {
            return convertJsonNode(current.get(0));
        }
        List<Object> values = new ArrayList<>();
        for (JsonNode node : current) {
            values.add(convertJsonNode(node));
        }
        return values;
    }

    /**
     * 将路径表达式按 '.' 分割为 token 列表（方括号内的 '.' 不分割）
     *
     * @param expression 表达式。
     * @return 路径片段列表。
     */
    private List<String> splitPathTokens(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '[') {
                bracketDepth++;
            } else if (ch == ']') {
                bracketDepth--;
            }
            if (ch == '.' && bracketDepth == 0) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
                continue;
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    /**
     * 将 JSON 节点转换为可序列化对象。
     *
     * @param node 节点定义。
     * @return 转换后的 Java 值。
     */
    private Object convertJsonNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isInt() || node.isLong()) {
            return node.asLong();
        }
        if (node.isFloat() || node.isDouble() || node.isBigDecimal()) {
            return node.asDouble();
        }
        return objectMapper.convertValue(node, Object.class);
    }

    /**
     * 计算参数映射列表的 SHA-256 哈希值（用于 Schema 缓存失效判断）
     *
     * @param mappings 参数映射列表。
     * @return 参数映射摘要。
     */
    private String computeMappingHash(List<McpToolMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return sha256("EMPTY");
        }
        List<String> lines = new ArrayList<>();
        for (McpToolMapping mapping : mappings) {
            if (mapping == null) {
                continue;
            }
            lines.add(String.join("|",
                    stringValue(mapping.getParentId()),
                    stringValue(mapping.getFieldName()),
                    stringValue(mapping.getMcpType()),
                    stringValue(mapping.getMcpDesc()),
                    stringValue(mapping.getIsRequired()),
                    stringValue(mapping.getItemType()),
                    stringValue(mapping.getItemRefId()),
                    stringValue(mapping.getHttpPath()),
                    stringValue(mapping.getHttpLocation()),
                    stringValue(mapping.getSortOrder())
            ));
        }
        Collections.sort(lines);
        return sha256(String.join("\n", lines));
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte aByte : bytes) {
                builder.append(String.format("%02x", aByte));
            }
            return builder.toString();
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 从映射节点向上遍历父节点，拼接完整的源路径（用于从 arguments 中读取值）
     *
     * @param mapping 字段映射配置。
     * @param nodeMap 字段节点映射。
     * @return 映射后的字段值。
     */
    private String resolveSourcePath(McpToolMapping mapping, Map<Long, McpToolMapping> nodeMap) {
        List<String> names = new ArrayList<>();
        McpToolMapping current = mapping;
        int guard = 0;
        while (current != null && guard < 16) {
            if (StringUtils.hasText(current.getFieldName())) {
                names.add(current.getFieldName());
            }
            Long parentId = current.getParentId();
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                break;
            }
            current = nodeMap.get(parentId);
            guard++;
        }
        Collections.reverse(names);
        String path = String.join(".", names);
        if (path.startsWith("arguments.")) {
            return path.substring("arguments.".length());
        }
        if ("arguments".equals(path)) {
            return "";
        }
        return path;
    }

    /**
     * 按路径从 Map 中读取嵌套值（支持 '.' 分隔和数组下标）
     *
     * @param source 源参数映射。
     * @param path   路径。
     * @return 路径命中的值。
     */
    private Object readValueByPath(Map<String, Object> source, String path) {
        if (source == null) {
            return null;
        }
        if (!StringUtils.hasText(path)) {
            return source;
        }

        List<String> tokens = splitPathTokens(path);
        Object current = source;
        for (String token : tokens) {
            if (current == null) {
                return null;
            }
            String fieldName = token;
            String indexExpr = null;
            int idxStart = token.indexOf('[');
            if (idxStart >= 0 && token.endsWith("]")) {
                fieldName = token.substring(0, idxStart);
                indexExpr = token.substring(idxStart + 1, token.length() - 1);
            }

            if (StringUtils.hasText(fieldName)) {
                if (!(current instanceof Map<?, ?> currentMap)) {
                    return null;
                }
                current = currentMap.get(fieldName);
            }

            if (indexExpr == null) {
                continue;
            }

            if (!(current instanceof List<?> currentList)) {
                return null;
            }
            if ("*".equals(indexExpr)) {
                return currentList;
            }
            int index = Integer.parseInt(indexExpr);
            if (index < 0 || index >= currentList.size()) {
                return null;
            }
            current = currentList.get(index);
        }
        return current;
    }

    /**
     * 按路径向 Map 中写入嵌套值（自动创建中间层 Map）
     *
     * @param target 目标参数映射。
     * @param path   路径。
     * @param value  写入值。
     */
    @SuppressWarnings("unchecked")
    private void setPathValue(Map<String, Object> target, String path, Object value) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        String[] segments = path.split("\\.");
        Map<String, Object> current = target;
        for (int i = 0; i < segments.length; i++) {
            String rawKey = segments[i];
            String key = rawKey.replaceAll("\\[.*?]", "");
            if (!StringUtils.hasText(key)) {
                continue;
            }
            boolean isLast = i == segments.length - 1;
            if (isLast) {
                current.put(key, value);
                return;
            }
            Object nested = current.get(key);
            if (!(nested instanceof Map<?, ?>)) {
                nested = new LinkedHashMap<String, Object>();
                current.put(key, nested);
            }
            current = (Map<String, Object>) nested;
        }
    }

    /**
     * 替换 URL 中的路径变量（支持 {key} 和 :key 两种风格）
     *
     * @param url   URL 地址。
     * @param key   键名。
     * @param value 查询参数值。
     * @return 追加查询参数后的 URL。
     */
    private String replacePathVariable(String url, String key, Object value) {
        if (!StringUtils.hasText(url) || !StringUtils.hasText(key) || value == null) {
            return url;
        }
        String encoded = URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
        String replaced = url.replace("{" + key + "}", encoded);
        return replaced.replace(":" + key, encoded);
    }

    /**
     * 将 query 参数拼接到 URL 上
     *
     * @param url   URL 地址。
     * @param query 查询参数集合。
     * @return 追加查询参数后的 URL。
     */
    private String buildFinalUrl(String url, Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return url;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    builder.queryParam(entry.getKey(), item);
                }
                continue;
            }
            builder.queryParam(entry.getKey(), value);
        }
        return builder.build(true).toUriString();
    }

    /**
     * 合并并应用 HTTP 请求头。
     *
     * @param headers       请求头集合。
     * @param sourceHeaders 原始请求头映射。
     */
    private void applyHeaders(HttpHeaders headers, Map<String, String> sourceHeaders) {
        if (headers == null || sourceHeaders == null || sourceHeaders.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : sourceHeaders.entrySet()) {
            if (StringUtils.hasText(entry.getKey()) && entry.getValue() != null) {
                headers.add(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 解析工具配置中的 JSON 格式请求头
     *
     * @param headersJson 工具参数 JSON。
     */
    private Map<String, String> parseHeaders(String headersJson) {
        if (!StringUtils.hasText(headersJson)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(headersJson, new TypeReference<>() {
            });
            Map<String, String> headers = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
                    continue;
                }
                headers.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return headers;
        } catch (Exception e) {
            log.warn("解析工具请求头失败: {}", headersJson, e);
            return new LinkedHashMap<>();
        }
    }

    private HttpMethod resolveHttpMethod(String method) {
        String normalized = StringUtils.hasText(method) ? method.trim().toUpperCase(Locale.ROOT) : "POST";
        try {
            return HttpMethod.valueOf(normalized);
        } catch (Exception e) {
            throw new BusinessException("不支持的 HTTP 方法: " + normalized);
        }
    }

    private boolean supportsBody(HttpMethod method) {
        return HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method);
    }

    /**
     * 归一化重试次数。
     *
     * @param retryTimes 重试次数
     * @return 归一化后的重试次数
     */
    private int normalizeRetryTimes(Integer retryTimes) {
        if (retryTimes == null || retryTimes < 0) {
            return DEFAULT_RETRY_TIMES;
        }
        return retryTimes;
    }

    /**
     * 归一化超时。
     *
     * @param timeout 超时时间
     * @return 归一化后的超时时间（毫秒）
     */
    private int normalizeTimeout(Integer timeout) {
        if (timeout == null || timeout <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return timeout;
    }

    /**
     * 计算安全的排序值。
     *
     * @param mapping 字段映射
     * @return 排序序号（空值时返回 0）
     */
    private int safeSortOrder(McpToolMapping mapping) {
        if (mapping == null || mapping.getSortOrder() == null) {
            return 0;
        }
        return mapping.getSortOrder();
    }

    /**
     * 归一化 Schema 类型。
     *
     * @param type 类型标识
     * @return 归一化结果
     */
    private String normalizeSchemaType(String type) {
        if (!StringUtils.hasText(type)) {
            return "string";
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if ("integer".equals(normalized)) {
            return "number";
        }
        if (!Set.of("string", "number", "boolean", "object", "array").contains(normalized)) {
            return "string";
        }
        return normalized;
    }

    /**
     * 构建 JSON Schema 的 object 节点。
     *
     * @param properties 对象属性定义。
     * @param required   必填字段列表。
     * @return object 类型的 schema 映射。
     */
    private Map<String, Object> buildObjectSchema(Map<String, Object> properties, List<String> required) {
        // 使用有序 Map，确保序列化后的字段顺序稳定（便于调试与比对）。
        Map<String, Object> schema = new LinkedHashMap<>();
        // 固定声明当前 schema 节点类型为 object。
        schema.put("type", "object");
        // 写入对象属性定义；入参为空时兜底为空对象，避免上层判空分支。
        schema.put("properties", properties == null ? new LinkedHashMap<>() : properties);
        // 仅在存在必填字段时输出 required，避免产生空数组噪音。
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    /**
     * 解析 JSON 映射。
     *
     * @param text 原始文本
     */
    private Map<String, Object> parseJsonMap(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("解析 JSON 失败，text: {}", text, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 将对象序列化为JSON 字符串。
     *
     * @param value 值
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 构建工具调用成功结果。
     *
     * @param callId    调用ID。
     * @param gatewayId 网关ID。
     * @param toolName  工具名称。
     * @param content   用户输入内容。
     * @param timeoutMs 超时时间（毫秒）。
     * @param startAt   开始时间戳。
     * @param arguments 工具入参。
     * @return 工具调用结果。
     */
    private ToolCallResult buildSuccessResult(String callId,
                                              String gatewayId,
                                              String toolName,
                                              String content,
                                              Integer timeoutMs,
                                              long startAt,
                                              Map<String, Object> arguments) {
        recordMetrics(gatewayId, toolName, true, null, timeoutMs, startAt);
        long latencyMs = Duration.ofNanos(System.nanoTime() - startAt).toMillis();
        log.info("gateway_tool_call source=SERVICE stage=end callId={} gatewayId={} toolName={} argsKeys={} success=true errorCode={} latencyMs={}",
                callId,
                gatewayId,
                toolName,
                arguments == null ? Collections.emptySet() : arguments.keySet(),
                "",
                latencyMs);
        return new ToolCallResult(true, content, null);
    }

    /**
     * 构建工具调用失败结果。
     *
     * @param callId    调用ID。
     * @param gatewayId 网关ID。
     * @param toolName  工具名称。
     * @param content   用户输入内容。
     * @param errorCode 错误码。
     * @param timeoutMs 超时时间（毫秒）。
     * @param startAt   开始时间戳。
     * @param arguments 工具入参。
     * @return 工具调用结果。
     */
    private ToolCallResult buildFailureResult(String callId,
                                              String gatewayId,
                                              String toolName,
                                              String content,
                                              String errorCode,
                                              Integer timeoutMs,
                                              long startAt,
                                              Map<String, Object> arguments) {
        recordMetrics(gatewayId, toolName, false, errorCode, timeoutMs, startAt);
        long latencyMs = Duration.ofNanos(System.nanoTime() - startAt).toMillis();
        log.info("gateway_tool_call source=SERVICE stage=end callId={} gatewayId={} toolName={} argsKeys={} success=false errorCode={} latencyMs={}",
                callId,
                gatewayId,
                toolName,
                arguments == null ? Collections.emptySet() : arguments.keySet(),
                errorCode,
                latencyMs);
        return new ToolCallResult(false, content, errorCode);
    }

    /**
     * 记录工具调用指标到可观测性服务
     *
     * @param gatewayId 标识 ID。
     * @param toolName  工具名称。
     * @param success   boolean 参数。
     * @param errorCode 编码。
     * @param timeoutMs 超时时间（毫秒）。
     * @param startAt   long 参数。
     */
    private void recordMetrics(String gatewayId,
                               String toolName,
                               boolean success,
                               String errorCode,
                               Integer timeoutMs,
                               long startAt) {
        long latencyMs = Duration.ofNanos(System.nanoTime() - startAt).toMillis();
        gatewayObservabilityAppService.recordCall(
                new GatewayObservabilityAppService.CallRecord(
                        gatewayId,
                        toolName,
                        success,
                        errorCode,
                        latencyMs,
                        timeoutMs
                )
        );
    }

    /**
     * 根据异常信息分类错误码（超时 vs 通用失败）
     *
     * @param e 异常信息。
     * @return 异常堆栈文本。
     */
    private String classifyErrorCode(Exception e) {
        if (e == null || !StringUtils.hasText(e.getMessage())) {
            return "TOOL_EXEC_FAILED";
        }
        String message = e.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("timeout") || message.contains("timed out")) {
            return "TOOL_EXEC_TIMEOUT";
        }
        return "TOOL_EXEC_FAILED";
    }

    private String resolveCallId() {
        String callId = MDC.get(CALL_ID_MDC_KEY);
        if (StringUtils.hasText(callId)) {
            return callId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * HTTP 调用载荷，封装 URL、方法、请求头、查询参数和请求体
     */
    private static class HttpInvokePayload {

        /**
         * 请求 URL。
         */
        private String url;

        /**
         * HTTP 方法。
         */
        private HttpMethod method;

        /**
         * 请求头参数。
         */
        private Map<String, String> headers;

        /**
         * Query 参数。
         */
        private Map<String, Object> query;

        /**
         * 请求体参数。
         */
        private Map<String, Object> body;
    }
}
