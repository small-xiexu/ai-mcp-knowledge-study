package com.xbk.knowledge.application.model.dto;

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
 * 职责：应用层命令/结果模型，用于传递用例输入输出
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICallCommand {

    /**
     * 请求内容
     *
     * 为什么：承载用户输入或业务指令
     */
    private String content;

    /**
     * 系统提示词（可选）
     *
     * 为什么：用于约束模型输出风格
     */
    private String systemPrompt;

    /**
     * 模型参数（可选）
     * 例如：temperature、maxTokens 等
     *
     * 为什么：支持动态控制模型行为
     */
    private Map<String, Object> parameters;

    /**
     * 是否启用流式输出（可选）
     *
     * 为什么：支持流式响应场景
     */
    private Boolean streaming;

    /**
     * 指定模型ID（可选）
     * 如果指定，将优先使用该模型
     *
     * 为什么：允许强制指定模型
     */
    private Long modelId;

    /**
     * 会话ID（可选）
     * 用于对话上下文记忆
     *
     * 为什么：用于多轮对话上下文
     */
    private Long sessionId;

    /**
     * 知识库标签列表（可选）
     *
     * 为什么：用于 RAG 检索过滤
     */
    private List<String> ragTags;
}
