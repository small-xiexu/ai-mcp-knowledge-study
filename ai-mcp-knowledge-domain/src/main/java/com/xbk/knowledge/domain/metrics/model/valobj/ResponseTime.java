package com.xbk.knowledge.domain.metrics.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 响应时间统计值对象
 *
 * 职责：领域值对象，用于表达统计结果等不可变语义
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseTime {
    /**
     * 模型 ID
     *
     * 标识统计归属的模型
     */
    private Long modelId;

    /**
     * 模型名称
     *
     * 用于展示
     */
    private String modelName;

    /**
     * 平均响应时间
     *
     * 衡量性能趋势
     */
    private Double avgResponseTime;

    /**
     * 最小响应时间
     *
     * 衡量性能底线
     */
    private Long minResponseTime;

    /**
     * 最大响应时间
     *
     * 衡量性能峰值
     */
    private Long maxResponseTime;
}
