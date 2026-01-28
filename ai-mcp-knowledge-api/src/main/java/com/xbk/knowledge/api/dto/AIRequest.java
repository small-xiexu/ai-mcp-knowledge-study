package com.xbk.knowledge.api.dto;

import com.xbk.knowledge.types.enums.ModelSelectionStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI 请求对象
 * 统一的 AI 模型调用请求参数
 *
 * @author xiexu
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
     * 任务类型（可选）
     * 如果指定，将根据任务类型自动选择模型
     */
    private String taskType;

    /**
     * 系统提示词（可选）
     */
    private String systemPrompt;

    /**
     * 模型参数（可选）
     * 例如：temperature、maxTokens 等
     */
    private Map<String, Object> parameters;

    /**
     * 模型选择策略（可选）
     * 如果不指定，将使用默认策略或根据任务类型选择
     */
    private ModelSelectionStrategy strategy;

    /**
     * 是否启用流式输出（可选）
     */
    private Boolean streaming;
}
