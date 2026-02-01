package com.xbk.knowledge.domain.model.aggregate.model;

import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 模型配置聚合
 * 以模型配置为聚合根，统一管理模型能力的生命周期
 *
 * 职责：聚合根承载一致性边界，保证模型与能力配置同步变更
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigAggregate {

    /**
     * 模型配置（聚合根）
     *
     * 为什么：聚合根控制模型配置生命周期
     */
    private ModelConfig modelConfig;

    /**
     * 模型能力（聚合内实体）
     *
     * 为什么：与模型配置保持一致性边界
     */
    private ModelCapability modelCapability;
}
