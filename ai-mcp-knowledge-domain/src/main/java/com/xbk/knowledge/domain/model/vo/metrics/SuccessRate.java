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
    private Long modelId;
    private String modelName;
    private Long totalCalls;
    private Long successCalls;
    private Double successRate;
}
