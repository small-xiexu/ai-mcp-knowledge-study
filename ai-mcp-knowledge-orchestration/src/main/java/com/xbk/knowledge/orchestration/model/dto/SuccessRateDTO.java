package com.xbk.knowledge.orchestration.model.dto;

import java.io.Serializable;

/**
 * 成功率统计 DTO
 * 用于统一输出成功率指标，避免重复计算与口径不一致
 *
 * @author xiexu
 */
public record SuccessRateDTO(
        /**
         * 总调用次数，用于成功率分母
         */
        Long totalCalls,
        /**
         * 成功调用次数，用于成功率分子
         */
        Long successCalls,
        /**
         * 成功率百分比，便于前端直接展示
         */
        Double successRate
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
