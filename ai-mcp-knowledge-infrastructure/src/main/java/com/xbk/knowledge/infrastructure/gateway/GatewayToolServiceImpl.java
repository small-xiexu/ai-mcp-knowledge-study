package com.xbk.knowledge.infrastructure.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
     * HTTP 请求载荷构建器。
     */
    private final GatewayHttpPayloadBuilder gatewayHttpPayloadBuilder;

    /**
     * HTTP 调用执行器。
     */
    private final GatewayHttpInvoker gatewayHttpInvoker;

    /**
     * HTTP 响应提取器。
     */
    private final GatewayHttpResponseExtractor gatewayHttpResponseExtractor;

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
        // 阶段 1：初始化调用链路上下文（计时起点 + 调用ID + 参数兜底）。
        long startAt = System.nanoTime();
        String callId = resolveCallId();
        Map<String, Object> safeArguments = arguments == null ? Collections.emptyMap() : arguments;
        // 先记录 start 日志，便于与 end/exception 日志做一一对应排查。
        log.info("gateway_tool_call source=SERVICE stage=start callId={} gatewayId={} toolName={} argsKeys={}",
                callId,
                gatewayId,
                toolName,
                safeArguments.keySet());

        // 阶段 2：基础参数校验，不满足直接按统一失败结构返回。
        if (!StringUtils.hasText(gatewayId) || !StringUtils.hasText(toolName)) {
            return buildFailureResult(callId, gatewayId, toolName, "工具调用参数不完整", "INVALID_PARAMS", null, startAt, safeArguments);
        }

        // 阶段 3：按 gatewayId + toolName 定位工具注册记录。
        Optional<McpToolRegistry> toolOptional = toolRegistryRepository.findByGatewayIdAndToolName(
                new ToolNameQuery(gatewayId, toolName)
        );
        // 工具不存在时返回 TOOL_NOT_FOUND，避免继续走后续映射与HTTP调用。
        if (toolOptional.isEmpty()) {
            return buildFailureResult(callId, gatewayId, toolName, "工具不存在: " + toolName, "TOOL_NOT_FOUND", null, startAt, safeArguments);
        }

        McpToolRegistry tool = toolOptional.get();
        // 仅启用态工具允许调用；禁用态统一返回 TOOL_DISABLED。
        if (tool.getStatus() == null || tool.getStatus() != 1) {
            return buildFailureResult(callId, gatewayId, toolName, "工具未启用: " + toolName, "TOOL_DISABLED", tool.getTimeout(), startAt, safeArguments);
        }

        // 阶段 4：加载 request/response 映射，分别用于入参与响应提取。
        List<McpToolMapping> requestMappings = toolMappingRepository.findByToolIdAndMappingType(
                new ToolMappingQuery(tool.getId(), REQUEST_MAPPING_TYPE)
        );
        List<McpToolMapping> responseMappings = toolMappingRepository.findByToolIdAndMappingType(
                new ToolMappingQuery(tool.getId(), RESPONSE_MAPPING_TYPE)
        );

        try {
            // 阶段 5：调用专用组件构建 HTTP 载荷（url/query/header/body/path）。
            GatewayHttpInvokePayload payload = gatewayHttpPayloadBuilder.buildInvokePayload(tool, requestMappings, safeArguments);
            // 阶段 6：调用专用执行器发起 HTTP 请求（含重试和超时控制）。
            String responseText = gatewayHttpInvoker.executeWithRetry(payload, tool.getRetryTimes(), tool.getTimeout());
            // 阶段 7：调用专用提取器按映射规则提取响应字段。
            String finalResult = gatewayHttpResponseExtractor.extractResponse(responseText, responseMappings);
            // 成功路径统一走 buildSuccessResult，内部会补齐指标与结束日志。
            return buildSuccessResult(callId, gatewayId, toolName, finalResult, tool.getTimeout(), startAt, safeArguments);
        } catch (Exception e) {
            // 异常路径统一分类错误码后返回，保证上层收到稳定的失败结构。
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

}
