package com.xbk.knowledge.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain 层 AI 响应对象
 * 统一的 AI 模型调用响应结果
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainAIResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 使用的模型名称
     */
    private String modelUsed;

    /**
     * 使用的 token 数量
     */
    private Integer tokensUsed;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 是否使用了降级模型
     */
    private Boolean fallback;

    /**
     * 重试次数
     */
    private Integer retryCount;
}
