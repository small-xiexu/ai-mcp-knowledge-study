package com.xbk.knowledge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 调用次数统计 DTO
 * 用于承载聚合统计结果，避免在接口层重复计算
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallMetricsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总调用次数，用于衡量整体调用规模
     */
    private Long totalCalls;

    /**
     * 成功调用次数，用于计算成功率
     */
    private Long successCalls;

    /**
     * 失败调用次数，用于分析失败占比
     */
    private Long failedCalls;

    /**
     * 降级调用次数，用于评估容错比例
     */
    private Long fallbackCalls;
}
