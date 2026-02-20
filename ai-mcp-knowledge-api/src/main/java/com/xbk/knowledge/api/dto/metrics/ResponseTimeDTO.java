package com.xbk.knowledge.api.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 响应时间统计 DTO
 * 用于统一响应耗时统计口径，便于性能分析
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseTimeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 平均响应时间（毫秒），用于总体性能评估
     */
    private Double avgResponseTime;

    /**
     * 最大响应时间（毫秒），用于识别长尾
     */
    private Long maxResponseTime;

    /**
     * 最小响应时间（毫秒），用于基线对比
     */
    private Long minResponseTime;
}
