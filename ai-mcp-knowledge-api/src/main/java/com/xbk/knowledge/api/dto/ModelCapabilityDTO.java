package com.xbk.knowledge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 模型能力 DTO
 * 用于 API 响应中的模型能力信息
 *
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCapabilityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 最大输入token
     */
    private Integer maxInputTokens;

    /**
     * 最大输出token
     */
    private Integer maxOutputTokens;

    /**
     * 支持函数调用
     */
    private Boolean supportFunctionCalling;

    /**
     * 支持视觉
     */
    private Boolean supportVision;

    /**
     * 支持流式输出
     */
    private Boolean supportStreaming;

    /**
     * 质量评分（1-100）
     */
    private Integer qualityScore;
}
