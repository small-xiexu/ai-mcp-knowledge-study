package com.xbk.knowledge.domain.metrics.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成功率统计值对象
 *
 * 职责：领域值对象，用于表达统计结果等不可变语义
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessRate {
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
     * 总调用次数
     *
     * 计算成功率的分母
     */
    private Long totalCalls;

    /**
     * 成功调用次数
     *
     * 计算成功率的分子
     */
    private Long successCalls;

    /**
     * 成功率
     *
     * 衡量可用性
     */
    private Double successRate;
}
