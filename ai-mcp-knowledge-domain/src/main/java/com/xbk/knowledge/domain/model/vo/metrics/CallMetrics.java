package com.xbk.knowledge.domain.model.vo.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调用统计值对象
 *
 * 职责：领域值对象，用于表达统计结果等不可变语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallMetrics {
    /**
     * 总调用次数
     *
     * 为什么：衡量总体调用规模
     */
    private Long totalCalls;

    /**
     * 成功调用次数
     *
     * 为什么：用于成功率统计
     */
    private Long successCalls;

    /**
     * 失败调用次数
     *
     * 为什么：用于失败率统计与告警
     */
    private Long failedCalls;

    /**
     * 平均响应时间
     *
     * 为什么：衡量性能趋势
     */
    private Double avgResponseTime;

    /**
     * 总 token 数
     *
     * 为什么：用于成本与限额统计
     */
    private Long totalTokens;
}
