package com.xbk.knowledge.domain.metrics.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 时间范围查询条件值对象
 * 统一承载时间区间筛选条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeRangeQuery {

    /**
     * 开始时间
     *
     * 限定查询时间范围
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     *
     * 限定查询时间范围
     */
    private LocalDateTime endTime;
}
