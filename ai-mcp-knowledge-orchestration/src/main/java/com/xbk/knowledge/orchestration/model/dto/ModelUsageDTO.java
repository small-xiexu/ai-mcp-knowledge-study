package com.xbk.knowledge.orchestration.model.dto;

import java.io.Serializable;

/**
 * 模型使用分布 DTO
 * 用于按模型维度展示调用分布，便于资源规划
 *
 * @author xiexu
 */
public record ModelUsageDTO(
        /**
         * 模型ID，用于标识模型
         */
        Long modelId,
        /**
         * 调用次数，用于计算占比
         */
        Long callCount,
        /**
         * 使用占比（百分比），便于直观展示
         */
        Double usageRate
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
