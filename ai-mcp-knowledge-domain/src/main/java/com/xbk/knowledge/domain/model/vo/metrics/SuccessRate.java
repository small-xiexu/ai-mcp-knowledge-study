package com.xbk.knowledge.domain.model.vo.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成功率统计值对象
 *
 * 职责：领域值对象，用于表达统计结果等不可变语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessRate {
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
     * 总调用次数
     *
     * 为什么：计算成功率的分母
     */
    private Long totalCalls;

    /**
     * 成功调用次数
     *
     * 为什么：计算成功率的分子
     */
    private Long successCalls;

    /**
     * 成功率
     *
     * 为什么：衡量可用性
     */
    private Double successRate;
}
