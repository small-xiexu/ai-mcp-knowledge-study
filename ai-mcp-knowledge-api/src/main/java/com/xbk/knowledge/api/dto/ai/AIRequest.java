package com.xbk.knowledge.api.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI 请求对象
 * 统一的 AI 模型调用请求参数
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {

    /**
     * 请求内容
     */
    private String content;

    /**
     * 系统提示词（可选）
     */
    private String systemPrompt;

    /**
     * 模型参数（可选）
     * 例如temperature、maxTokens 等
     */
    private Map<String, Object> parameters;

    /**
     * 是否启用流式输出（可选）
     */
    private Boolean streaming;

    /**
     * 指定模型ID（可选）
     * 如果指定，将优先使用该模型
     */
    private Long modelId;

    /**
     * 会话 ID（可选）
     * 用于对话上下文记忆
     */
    private Long sessionId;

    /**
     * 知识库标签列表（可选）
     * 用于 RAG 检索过滤
     */
    private List<String> ragTags;
}
