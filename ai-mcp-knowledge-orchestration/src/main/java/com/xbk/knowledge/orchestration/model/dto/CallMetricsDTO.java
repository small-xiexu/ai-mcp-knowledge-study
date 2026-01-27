package com.xbk.knowledge.orchestration.model.dto;

import java.io.Serializable;

/**
 * 调用次数统计 DTO
 * 用于承载聚合统计结果，避免在接口层重复计算
 *
 * @author xiexu
 */
public record CallMetricsDTO(
        /**
         * 总调用次数，用于衡量整体调用规模
         */
        Long totalCalls,
        /**
         * 成功调用次数，用于计算成功率
         */
        Long successCalls,
        /**
         * 失败调用次数，用于分析失败占比
         */
        Long failedCalls,
        /**
         * 降级调用次数，用于评估容错比例
         */
        Long fallbackCalls
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
