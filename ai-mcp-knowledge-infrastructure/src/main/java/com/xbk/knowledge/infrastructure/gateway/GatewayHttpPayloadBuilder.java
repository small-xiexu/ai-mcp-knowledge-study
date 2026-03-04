package com.xbk.knowledge.infrastructure.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gateway HTTP 请求载荷构建器。
 *
 * 职责：根据工具配置与映射规则，将模型参数转换为可执行 HTTP 载荷。
 *
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHttpPayloadBuilder {

    /**
     * JSON 序列化/反序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构建 HTTP 调用载荷。
     *
     * @param tool            工具注册配置。
     * @param requestMappings 请求参数映射列表。
     * @param arguments       工具调用参数。
     * @return HTTP 调用载荷。
     */
    public GatewayHttpInvokePayload buildInvokePayload(McpToolRegistry tool,
                                                       List<McpToolMapping> requestMappings,
                                                       Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments == null ? Collections.emptyMap() : arguments;

        GatewayHttpInvokePayload payload = new GatewayHttpInvokePayload();
        payload.setUrl(tool.getHttpUrl());
        payload.setMethod(resolveHttpMethod(tool.getHttpMethod()));
        payload.setHeaders(parseHeaders(tool.getHttpHeaders()));
        payload.setQuery(new LinkedHashMap<>());
        payload.setBody(new LinkedHashMap<>());

        if (requestMappings == null || requestMappings.isEmpty()) {
            if (supportsBody(payload.getMethod())) {
                payload.getBody().putAll(safeArguments);
            } else {
                payload.getQuery().putAll(safeArguments);
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
            Object value = readValueByPath(safeArguments, sourcePath);
            if (value == null && StringUtils.hasText(mapping.getFieldName())) {
                value = safeArguments.get(mapping.getFieldName());
            }
            if (value == null) {
                continue;
            }
            applyValueToPayload(payload, mapping.getHttpLocation(), mapping.getHttpPath(), value);
        }

        return payload;
    }

    /**
     * 将参数值写入 HTTP 载荷的指定位置。
     *
     * @param payload      HTTP 调用载荷。
     * @param httpLocation 映射落点（header/query/path/body）。
     * @param httpPath     目标路径。
     * @param value        参数值。
     */
    private void applyValueToPayload(GatewayHttpInvokePayload payload,
                                     String httpLocation,
                                     String httpPath,
                                     Object value) {
        String location = httpLocation.toLowerCase(Locale.ROOT);
        if ("header".equals(location)) {
            payload.getHeaders().put(httpPath, String.valueOf(value));
            return;
        }

        if ("query".equals(location)) {
            payload.getQuery().put(httpPath, value);
            return;
        }

        if ("path".equals(location)) {
            payload.setUrl(replacePathVariable(payload.getUrl(), httpPath, value));
            return;
        }

        setPathValue(payload.getBody(), httpPath, value);
    }

    /**
     * 从映射节点向上遍历父节点，拼接完整源路径。
     *
     * @param mapping 字段映射配置。
     * @param nodeMap 字段节点映射。
     * @return 源路径。
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
     * 按路径从 Map 中读取嵌套值。
     *
     * @param source 源参数映射。
     * @param path   路径。
     * @return 命中的值。
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
     * 按路径向 Map 中写入嵌套值。
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
     * 替换 URL 中路径变量（支持 {key} 与 :key）。
     *
     * @param url   URL 地址。
     * @param key   键名。
     * @param value 变量值。
     * @return 替换后的 URL。
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
     * 解析工具配置中的 JSON 请求头。
     *
     * @param headersJson 请求头 JSON。
     * @return 头部映射。
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

    /**
     * 解析 HTTP 方法。
     *
     * @param method 方法文本。
     * @return HTTP 方法。
     */
    private HttpMethod resolveHttpMethod(String method) {
        String normalized = StringUtils.hasText(method) ? method.trim().toUpperCase(Locale.ROOT) : "POST";
        try {
            return HttpMethod.valueOf(normalized);
        } catch (Exception e) {
            throw new BusinessException("不支持的 HTTP 方法: " + normalized);
        }
    }

    /**
     * 判断该 HTTP 方法是否支持请求体。
     *
     * @param method HTTP 方法。
     * @return true 表示支持 body。
     */
    private boolean supportsBody(HttpMethod method) {
        return HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method);
    }

    /**
     * 将路径表达式按 '.' 分割为 token 列表。
     *
     * @param expression 路径表达式。
     * @return token 列表。
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
}
