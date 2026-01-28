package com.xbk.knowledge.domain.model.dto;

import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain 层模型元信息
 * 用于返回模型的基本信息和能力
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainModelInfo {

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型类型
     */
    private ModelType modelType;

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
