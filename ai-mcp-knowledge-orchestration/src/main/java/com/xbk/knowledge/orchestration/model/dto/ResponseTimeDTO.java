package com.xbk.knowledge.orchestration.model.dto;

import java.io.Serializable;

/**
 * 响应时间统计 DTO
 * 用于统一响应耗时统计口径，便于性能分析
 *
 * @author xiexu
 */
public record ResponseTimeDTO(
        /**
         * 平均响应时间（毫秒），用于总体性能评估
         */
        Double avgResponseTime,
        /**
         * 最大响应时间（毫秒），用于识别长尾
         */
        Long maxResponseTime,
        /**
         * 最小响应时间（毫秒），用于基线对比
         */
        Long minResponseTime
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
