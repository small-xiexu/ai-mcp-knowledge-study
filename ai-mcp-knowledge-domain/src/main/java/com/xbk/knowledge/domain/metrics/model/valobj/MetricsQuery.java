package com.xbk.knowledge.domain.metrics.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 监控指标查询条件值对象
 * 统一承载指标统计的筛选条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricsQuery {

    /**
     * 模型 ID（可选）
     *
     * 按模型维度统计
     */
    private Long modelId;

    /**
     * 开始时间（可选）
     *
     * 限定统计时间范围
     */
    private LocalDateTime startTime;

    /**
     * 结束时间（可选）
     *
     * 限定统计时间范围
     */
    private LocalDateTime endTime;
}
