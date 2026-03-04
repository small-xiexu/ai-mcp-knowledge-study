package com.xbk.knowledge.infrastructure.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway HTTP 响应提取器。
 *
 * 职责：根据响应映射规则，从原始 JSON 响应中提取需要返回给模型的字段。
 *
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHttpResponseExtractor {

    /**
     * JSON 序列化/反序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 根据响应映射规则从原始响应中提取字段。
     *
     * @param rawResponse      原始响应。
     * @param responseMappings 响应字段映射列表。
     * @return 处理后的响应文本。
     */
    public String extractResponse(String rawResponse, List<McpToolMapping> responseMappings) {
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
     * 按路径表达式从 JSON 树中提取值。
     *
     * @param root       JSON 根节点。
     * @param expression 路径表达式。
     * @return 提取结果。
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
     * 将路径表达式按 '.' 分割为 token 列表。
     *
     * @param expression 表达式。
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

    /**
     * 将 JSON 节点转换为可序列化对象。
     *
     * @param node JSON 节点。
     * @return Java 值。
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
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 待序列化对象。
     * @return JSON 字符串。
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
}
