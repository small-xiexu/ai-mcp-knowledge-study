package com.xbk.knowledge.domain.model.vo.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型使用情况值对象
 *
 * 职责：领域值对象，用于表达统计结果等不可变语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsage {
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
     * 调用次数
     *
     * 为什么：衡量使用规模
     */
    private Long callCount;

    /**
     * 总 token 数
     *
     * 为什么：用于成本与限额统计
     */
    private Long totalTokens;
}
