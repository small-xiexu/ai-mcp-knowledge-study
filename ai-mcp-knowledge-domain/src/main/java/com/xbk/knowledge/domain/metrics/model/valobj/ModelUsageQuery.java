package com.xbk.knowledge.domain.metrics.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型使用分布查询条件值对象
 * 统一承载模型使用统计的时间范围
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsageQuery {

    /**
     * 开始时间（可选）
     *
     * 为什么：限定统计时间范围
     */
    private LocalDateTime startTime;

    /**
     * 结束时间（可选）
     *
     * 为什么：限定统计时间范围
     */
    private LocalDateTime endTime;
}
