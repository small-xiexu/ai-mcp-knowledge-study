package com.xbk.knowledge.domain.model.vo;

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
    private Long modelId;
    private String modelName;
    private Long callCount;
    private Long totalTokens;
}
