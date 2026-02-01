package com.xbk.knowledge.domain.model.vo.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 响应时间统计值对象
 *
 * 职责：领域值对象，用于表达统计结果等不可变语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseTime {
    /**
     * 模型 ID
     *
     * 为什么：标识统计归属的模型
     */
    private Long modelId;

    /**
     * 模型名称
     *
     * 为什么：用于展示
     */
    private String modelName;

    /**
     * 平均响应时间
     *
     * 为什么：衡量性能趋势
     */
    private Double avgResponseTime;

    /**
     * 最小响应时间
     *
     * 为什么：衡量性能底线
     */
    private Long minResponseTime;

    /**
     * 最大响应时间
     *
     * 为什么：衡量性能峰值
     */
    private Long maxResponseTime;
}
