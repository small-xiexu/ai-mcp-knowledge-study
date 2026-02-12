package com.xbk.knowledge.infrastructure.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.app.GatewayObservabilityAppService;
import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolMapping;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolRegistry;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolSchema;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolMappingQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolNameQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolMappingRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolRegistryRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolSchemaRepository;
import com.xbk.knowledge.domain.service.gateway.GatewayToolService;
import com.xbk.knowledge.types.exception.BusinessException;
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
 * Gateway 工具域服务实现
 *
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayToolServiceImpl implements GatewayToolService {

    private static final String CALL_ID_MDC_KEY = "gatewayToolCallId";
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String REQUEST_MAPPING_TYPE = "request";
    private static final String RESPONSE_MAPPING_TYPE = "response";
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final int DEFAULT_RETRY_TIMES = 0;

    private final McpGatewayRepository gatewayRepository;
    private final McpToolRegistryRepository toolRegistryRepository;
    private final McpToolMappingRepository toolMappingRepository;
    private final McpToolSchemaRepository toolSchemaRepository;
    private final GatewayObservabilityAppService gatewayObservabilityAppService;
    private final ObjectMapper objectMapper;

    private final WebClient webClient = WebClient.builder().build();

    @Override
    public List<ToolDefinition> listTools(String gatewayId) {
        McpGateway gateway = requireEnabledGateway(gatewayId);
        List<McpToolRegistry> tools = toolRegistryRepository.findEnabledByGatewayId(new GatewayIdQuery(gateway.getGatewayId()));
        if (tools == null || tools.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolDefinition> definitions = new ArrayList<>();
        for (McpToolRegistry tool : tools) {
            List<McpToolMapping> requestMappings = toolMappingRepository.findByToolIdAndMappingType(
                    new ToolMappingQuery(tool.getId(), REQUEST_MAPPING_TYPE)
            );
            Map<String, Object> inputSchema = resolveInputSchema(gatewayId, tool, requestMappings);
            definitions.add(new ToolDefinition(
                    tool.getToolName(),
                    StringUtils.hasText(tool.getToolDescription()) ? tool.getToolDescription() : "",
                    inputSchema
            ));
        }
        return definitions;
    }

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

    @Override
    public GatewayCapability initialize(String gatewayId) {
        McpGateway gateway = requireEnabledGateway(gatewayId);
        String serverName = StringUtils.hasText(gateway.getGatewayName()) ? gateway.getGatewayName() : gateway.getGatewayId();
        String serverVersion = StringUtils.hasText(gateway.getGatewayVersion()) ? gateway.getGatewayVersion() : "1.0.0";
        String instructions = StringUtils.hasText(gateway.getGatewayInstructions()) ? gateway.getGatewayInstructions() : "";
        return new GatewayCapability(PROTOCOL_VERSION, serverName, serverVersion, instructions);
    }

    private McpGateway requireEnabledGateway(String gatewayId) {
        if (!StringUtils.hasText(gatewayId)) {
            throw new BusinessException("gatewayId 不能为空");
        }
        Optional<McpGateway> gatewayOptional = gatewayRepository.findByGatewayId(new GatewayIdQuery(gatewayId));
        if (gatewayOptional.isEmpty()) {
            throw new BusinessException("网关不存在: " + gatewayId);
        }
        McpGateway gateway = gatewayOptional.get();
        if (gateway.getStatus() == null || gateway.getStatus() != 1) {
            throw new BusinessException("网关未启用: " + gatewayId);
        }
        return gateway;
    }

    private Map<String, Object> resolveInputSchema(String gatewayId,
                                                   McpToolRegistry tool,
                                                   List<McpToolMapping> mappings) {
        String mappingHash = computeMappingHash(mappings);
        Optional<McpToolSchema> cachedOptional = toolSchemaRepository.findActiveByGatewayIdAndToolId(gatewayId, tool.getId());
        if (cachedOptional.isPresent()) {
            McpToolSchema cached = cachedOptional.get();
            if (StringUtils.hasText(cached.getInputSchema())
                    && StringUtils.hasText(cached.getSchemaHash())
                    && cached.getSchemaHash().equals(mappingHash)) {
                Map<String, Object> cachedSchema = parseJsonMap(cached.getInputSchema());
                if (!cachedSchema.isEmpty()) {
                    return cachedSchema;
                }
            }
        }

        Map<String, Object> schema = buildInputSchema(mappings);
        saveInputSchemaCache(gatewayId, tool.getId(), mappingHash, schema, cachedOptional.orElse(null));
        return schema;
    }

    private void saveInputSchemaCache(String gatewayId,
                                      Long toolId,
                                      String mappingHash,
                                      Map<String, Object> schema,
                                      McpToolSchema oldSchema) {
        McpToolSchema schemaEntity = oldSchema == null ? new McpToolSchema() : oldSchema;
        schemaEntity.setGatewayId(gatewayId);
        schemaEntity.setToolId(toolId);
        int nextVersion = oldSchema == null || oldSchema.getSchemaVersion() == null
                ? 1
                : oldSchema.getSchemaVersion() + 1;
        schemaEntity.setSchemaVersion(nextVersion);
        schemaEntity.setInputSchema(toJson(schema));
        schemaEntity.setSchemaHash(mappingHash);
        schemaEntity.setIsActive(Boolean.TRUE);
        schemaEntity.setUpdatedAt(LocalDateTime.now());
        if (schemaEntity.getCreatedAt() == null) {
            schemaEntity.setCreatedAt(LocalDateTime.now());
        }
        toolSchemaRepository.save(schemaEntity);
    }

    private Map<String, Object> buildInputSchema(List<McpToolMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return buildObjectSchema(Collections.emptyMap(), Collections.emptyList());
        }

        Map<Long, McpToolMapping> nodeMap = new HashMap<>();
        Map<Long, List<McpToolMapping>> childrenMap = new HashMap<>();
        List<McpToolMapping> roots = new ArrayList<>();
        for (McpToolMapping mapping : mappings) {
            if (mapping == null || mapping.getId() == null) {
                continue;
            }
            nodeMap.put(mapping.getId(), mapping);
        }
        for (McpToolMapping mapping : mappings) {
            if (mapping == null || mapping.getId() == null) {
                continue;
            }
            Long parentId = mapping.getParentId();
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(mapping);
                continue;
            }
            childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(mapping);
        }

        roots.sort(Comparator.comparingInt(this::safeSortOrder));
        if (roots.size() == 1 && "arguments".equalsIgnoreCase(roots.get(0).getFieldName())) {
            return buildObjectSchemaFromChildren(roots.get(0).getId(), childrenMap, nodeMap, new HashSet<>());
        }
        return buildObjectSchemaFromNodes(roots, childrenMap, nodeMap, new HashSet<>());
    }

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

    private String executeOnce(HttpInvokePayload payload, int timeoutMs) {
        String finalUrl = buildFinalUrl(payload.url, payload.query);
        WebClient.RequestBodySpec request = webClient
                .method(payload.method)
                .uri(finalUrl)
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

    private String replacePathVariable(String url, String key, Object value) {
        if (!StringUtils.hasText(url) || !StringUtils.hasText(key) || value == null) {
            return url;
        }
        String encoded = URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
        String replaced = url.replace("{" + key + "}", encoded);
        return replaced.replace(":" + key, encoded);
    }

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

    private Map<String, String> parseHeaders(String headersJson) {
        if (!StringUtils.hasText(headersJson)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(headersJson, new TypeReference<>() {});
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

    private int normalizeRetryTimes(Integer retryTimes) {
        if (retryTimes == null || retryTimes < 0) {
            return DEFAULT_RETRY_TIMES;
        }
        return retryTimes;
    }

    private int normalizeTimeout(Integer timeout) {
        if (timeout == null || timeout <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return timeout;
    }

    private int safeSortOrder(McpToolMapping mapping) {
        if (mapping == null || mapping.getSortOrder() == null) {
            return 0;
        }
        return mapping.getSortOrder();
    }

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

    private Map<String, Object> buildObjectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties == null ? new LinkedHashMap<>() : properties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private Map<String, Object> parseJsonMap(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析 JSON 失败，text: {}", text, e);
            return Collections.emptyMap();
        }
    }

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

    private static class HttpInvokePayload {
        private String url;
        private HttpMethod method;
        private Map<String, String> headers;
        private Map<String, Object> query;
        private Map<String, Object> body;
    }
}
