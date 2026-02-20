package com.xbk.knowledge.api.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 模型使用分布 DTO
 * 用于按模型维度展示调用分布，便于资源规划
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型ID，用于标识模型
     */
    private Long modelId;

    /**
     * 调用次数，用于计算占比
     */
    private Long callCount;

    /**
     * 使用占比（百分比），便于直观展示
     */
    private Double usageRate;
}
