package com.xbk.knowledge.application.support.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * PlatformContractV1 输出解析与修复支持。
 *
 * 职责：
 * 1、 从模型输出的 JSON 文本解析为 PlatformContractV1（仅使用 answer/uncertainty/citations/toolCalls/actionsNext）
 * 2、 容错支持从代码块或夹杂文本中提取 JSON
 *
 * 说明：meta/status/error 由平台补齐，模型输出不可信。
 *
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformContractV1OutputSupport {
    /**
     * JSON 序列化组件，用于解析模型输出 JSON 文本。
     */
    private final ObjectMapper objectMapper;

    /**
     * v1 输出约束指令（追加到系统消息中，强制模型输出 JSON）。
     * 
     * @return 平台输出契约指令文本。
     */
    public String contractInstruction() {
        return """
                你必须仅输出一个合法的 JSON 对象，不要输出任何额外文本、解释、Markdown、代码块标记。
                JSON 结构要求（字段必须存在，缺省用空字符串或空数组）
                {
                  "answer": "string",
                  "uncertainty": "string",
                  "citations": [{"title":"string","snippet":"string","source":"string"}],
                  "toolCalls": [{"toolKey":"string","summary":"string","resultSnippet":"string"}],
                  "actionsNext": ["string"]
                }
                """;
    }

    /**
     * 解析模型输出为 v1（不含 meta/status）。
     * 
     * @param rawText 模型输出文本
     * @return 解析结果（解析失败返回 null）
     */
    public PlatformContractV1 parseOrNull(String rawText) {
        String json = extractJsonObject(rawText);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return null;
            }
            String answer = textOrEmpty(root.get("answer"));
            String uncertainty = textOrEmpty(root.get("uncertainty"));

            List<PlatformContractV1.Citation> citations = parseCitations(root.get("citations"));
            List<PlatformContractV1.ToolCall> toolCalls = parseToolCalls(firstNonNull(root.get("toolCalls"), root.get("tool_calls")));
            List<String> actionsNext = parseStringArray(firstNonNull(root.get("actionsNext"), root.get("actions_next")));

            return PlatformContractV1.builder()
                    .answer(answer)
                    .uncertainty(uncertainty)
                    .citations(citations)
                    .toolCalls(toolCalls)
                    .actionsNext(actionsNext)
                    .build();
        } catch (Exception e) {
            log.warn("解析 PlatformContractV1 JSON 失败", e);
            return null;
        }
    }

    /**
     * 提取 JSON 对象。
     * 
     * @param text 原始文本。
     * @return 提取出的 JSON 字符串。
     */
    private String extractJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String t = text.trim();
        // 去除 ```json ... ``` 代码块包装
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline > 0) {
                t = t.substring(firstNewline + 1);
            }
            int endFence = t.lastIndexOf("```");
            if (endFence >= 0) {
                t = t.substring(0, endFence);
            }
            t = t.trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }
        return t.substring(start, end + 1);
    }

    /**
     * 返回文本或空字符串。
     * 
     * @param node 节点定义。
     * @return 非空文本内容。
     */
    private String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        String v = node.asText();
        return v == null ? "" : v;
    }

    private JsonNode firstNonNull(JsonNode a, JsonNode b) {
        return a != null && !a.isNull() ? a : b;
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || node.isNull() || !node.isArray()) {
            return list;
        }
        for (JsonNode n : node) {
            if (n == null || n.isNull()) {
                continue;
            }
            String v = n.asText();
            if (StringUtils.hasText(v)) {
                list.add(v);
            }
        }
        return list;
    }

    private List<PlatformContractV1.Citation> parseCitations(JsonNode node) {
        List<PlatformContractV1.Citation> list = new ArrayList<>();
        if (node == null || node.isNull() || !node.isArray()) {
            return list;
        }
        for (JsonNode n : node) {
            if (n == null || n.isNull() || !n.isObject()) {
                continue;
            }
            list.add(PlatformContractV1.Citation.builder()
                    .title(textOrEmpty(n.get("title")))
                    .snippet(textOrEmpty(n.get("snippet")))
                    .source(textOrEmpty(n.get("source")))
                    .build());
        }
        return list;
    }

    private List<PlatformContractV1.ToolCall> parseToolCalls(JsonNode node) {
        List<PlatformContractV1.ToolCall> list = new ArrayList<>();
        if (node == null || node.isNull() || !node.isArray()) {
            return list;
        }
        for (JsonNode n : node) {
            if (n == null || n.isNull() || !n.isObject()) {
                continue;
            }
            list.add(PlatformContractV1.ToolCall.builder()
                    .toolKey(textOrEmpty(firstNonNull(n.get("toolKey"), n.get("tool_key"))))
                    .summary(textOrEmpty(n.get("summary")))
                    .resultSnippet(textOrEmpty(firstNonNull(n.get("resultSnippet"), n.get("result_snippet"))))
                    .build());
        }
        return list;
    }
}
