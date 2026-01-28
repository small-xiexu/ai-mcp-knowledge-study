package com.xbk.knowledge.orchestration.model.dto;

import com.xbk.knowledge.orchestration.domain.entity.ModelCapability;
import com.xbk.knowledge.orchestration.model.enums.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型元信息
 * 用于返回模型的基本信息和能力
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * API 提供商类型
     * 标识使用哪个提供商的 API 协议（OPENAI/ANTHROPIC/GEMINI）
     */
    private ProviderType providerType;

    /**
     * 质量评分
     */
    private Integer qualityScore;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 模型能力
     */
    private ModelCapability capability;
}
