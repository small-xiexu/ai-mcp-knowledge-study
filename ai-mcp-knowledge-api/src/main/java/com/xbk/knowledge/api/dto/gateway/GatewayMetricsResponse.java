package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Gateway 监控指标响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayMetricsResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 指标生成时间。
     */
    private LocalDateTime generatedAt;

    /**
     * 统计窗口（分钟）。
     */
    private Integer recentMinutes;

    /**
     * 工具指标快照列表。
     */
    private List<ToolMetricsSnapshot> toolMetrics;

    /**
     * 告警快照列表。
     */
    private List<AlertSnapshot> alerts;

    /**
     * 工具指标快照。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolMetricsSnapshot implements Serializable {

        /**
         * 序列化版本号。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 网关 ID。
         */
        private String gatewayId;

        /**
         * 工具名称。
         */
        private String toolName;

        /**
         * 请求总数。
         */
        private Long requestCount;

        /**
         * 成功率（0-1）。
         */
        private Double successRate;

        /**
         * P95 延迟（毫秒）。
         */
        private Long p95LatencyMs;

        /**
         * P99 延迟（毫秒）。
         */
        private Long p99LatencyMs;

        /**
         * 平均延迟（毫秒）。
         */
        private Double avgLatencyMs;

        /**
         * 错误码分布（errorCode -> count）。
         */
        private Map<String, Long> errorDistribution;

        /**
         * SLA 达标率（0-1）。
         */
        private Double slaRate;

        /**
         * 超时率（0-1）。
         */
        private Double timeoutRate;

        /**
         * 连续失败次数。
         */
        private Integer consecutiveFailures;

        /**
         * 最近一次调用时间。
         */
        private LocalDateTime latestCallAt;

        /**
         * 最近一次调用是否成功。
         */
        private Boolean latestCallSuccess;
    }

    /**
     * 告警快照。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertSnapshot implements Serializable {

        /**
         * 序列化版本号。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 告警类型。
         */
        private String alertType;

        /**
         * 告警级别。
         */
        private String level;

        /**
         * 网关 ID。
         */
        private String gatewayId;

        /**
         * 工具名称。
         */
        private String toolName;

        /**
         * 告警消息。
         */
        private String message;

        /**
         * 触发时间。
         */
        private LocalDateTime triggeredAt;
    }
}
