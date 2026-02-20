package com.xbk.knowledge.api.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 成功率统计 DTO
 * 用于统一输出成功率指标，避免重复计算与口径不一致
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessRateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总调用次数，用于成功率分母
     */
    private Long totalCalls;

    /**
     * 成功调用次数，用于成功率分子
     */
    private Long successCalls;

    /**
     * 成功率百分比，便于前端直接展示
     */
    private Double successRate;
}
