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
    private Long modelId;
    private String modelName;
    private Double avgResponseTime;
    private Long minResponseTime;
    private Long maxResponseTime;
}
