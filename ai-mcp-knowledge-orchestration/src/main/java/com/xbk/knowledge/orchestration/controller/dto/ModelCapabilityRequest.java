package com.xbk.knowledge.orchestration.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 模型能力请求 DTO
 * 用于配置模型的能力参数
 *
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCapabilityRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 最大token数
     */
    private Integer maxTokens;

    /**
     * 温度参数（0.0-2.0）
     */
    private Double temperature;

    /**
     * Top-P参数（0.0-1.0）
     */
    private Double topP;

    /**
     * 质量评分（0-100）
     */
    private Integer qualityScore;

    /**
     * 速度评分（0-100）
     */
    private Integer speedScore;

    /**
     * 成本评分（0-100）
     */
    private Integer costScore;
}
