package com.xbk.knowledge.application.service.app;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Gateway 可观测性应用服务
 * 统一维护工具级指标与告警快照，供内部/外部路径复用同一口径。
 *
 * @author sxie
 */
public interface GatewayObservabilityAppService {

    /**
     * 记录一次工具调用指标
     *
     * @param record 指标记录
     */
    void recordCall(CallRecord record);

    /**
     * 查询工具指标与告警快照
     * 
     * @param query 时间范围查询条件。
     * @return 观测报告
     */
    GatewayMetricsReport queryMetrics(MetricsQuery query);

    /**
     * 调用记录。
     */
    @lombok.EqualsAndHashCode
    @lombok.ToString
    final class CallRecord {
        private final String gatewayId;
        private final String toolName;
        private final boolean success;
        private final String errorCode;
        private final long latencyMs;
        private final Integer timeoutMs;

        public CallRecord(String gatewayId,
                          String toolName,
                          boolean success,
                          String errorCode,
                          long latencyMs,
                          Integer timeoutMs) {
            this.gatewayId = gatewayId;
            this.toolName = toolName;
            this.success = success;
            this.errorCode = errorCode;
            this.latencyMs = latencyMs;
            this.timeoutMs = timeoutMs;
        }

        public String getGatewayId() {
            return gatewayId;
        }

        public String gatewayId() {
            return gatewayId;
        }

        public String getToolName() {
            return toolName;
        }

        public String toolName() {
            return toolName;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean success() {
            return success;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String errorCode() {
            return errorCode;
        }

        public long getLatencyMs() {
            return latencyMs;
        }

        public long latencyMs() {
            return latencyMs;
        }

        public Integer getTimeoutMs() {
            return timeoutMs;
        }

        public Integer timeoutMs() {
            return timeoutMs;
        }
    }

    /**
     * 指标查询条件。
     */
    @lombok.EqualsAndHashCode
    @lombok.ToString
    final class MetricsQuery {
        private final String gatewayId;
        private final String toolName;
        private final Integer recentMinutes;

        public MetricsQuery(String gatewayId, String toolName, Integer recentMinutes) {
            this.gatewayId = gatewayId;
            this.toolName = toolName;
            this.recentMinutes = recentMinutes;
        }

        public String getGatewayId() {
            return gatewayId;
        }

        public String gatewayId() {
            return gatewayId;
        }

        public String getToolName() {
            return toolName;
        }

        public String toolName() {
            return toolName;
        }

        public Integer getRecentMinutes() {
            return recentMinutes;
        }

        public Integer recentMinutes() {
            return recentMinutes;
        }
    }

    /**
     * 工具指标快照。
     */
    @lombok.EqualsAndHashCode
    @lombok.ToString
    final class ToolMetricsSnapshot {
        private final String gatewayId;
        private final String toolName;
        private final long requestCount;
        private final double successRate;
        private final long p95LatencyMs;
        private final long p99LatencyMs;
        private final double avgLatencyMs;
        private final Map<String, Long> errorDistribution;
        private final double slaRate;
        private final double timeoutRate;
        private final int consecutiveFailures;
        private final LocalDateTime latestCallAt;
        private final boolean latestCallSuccess;

        public ToolMetricsSnapshot(String gatewayId,
                                   String toolName,
                                   long requestCount,
                                   double successRate,
                                   long p95LatencyMs,
                                   long p99LatencyMs,
                                   double avgLatencyMs,
                                   Map<String, Long> errorDistribution,
                                   double slaRate,
                                   double timeoutRate,
                                   int consecutiveFailures,
                                   LocalDateTime latestCallAt,
                                   boolean latestCallSuccess) {
            this.gatewayId = gatewayId;
            this.toolName = toolName;
            this.requestCount = requestCount;
            this.successRate = successRate;
            this.p95LatencyMs = p95LatencyMs;
            this.p99LatencyMs = p99LatencyMs;
            this.avgLatencyMs = avgLatencyMs;
            this.errorDistribution = errorDistribution;
            this.slaRate = slaRate;
            this.timeoutRate = timeoutRate;
            this.consecutiveFailures = consecutiveFailures;
            this.latestCallAt = latestCallAt;
            this.latestCallSuccess = latestCallSuccess;
        }

        public String getGatewayId() {
            return gatewayId;
        }

        public String gatewayId() {
            return gatewayId;
        }

        public String getToolName() {
            return toolName;
        }

        public String toolName() {
            return toolName;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public long requestCount() {
            return requestCount;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public double successRate() {
            return successRate;
        }

        public long getP95LatencyMs() {
            return p95LatencyMs;
        }

        public long p95LatencyMs() {
            return p95LatencyMs;
        }

        public long getP99LatencyMs() {
            return p99LatencyMs;
        }

        public long p99LatencyMs() {
            return p99LatencyMs;
        }

        public double getAvgLatencyMs() {
            return avgLatencyMs;
        }

        public double avgLatencyMs() {
            return avgLatencyMs;
        }

        public Map<String, Long> getErrorDistribution() {
            return errorDistribution;
        }

        public Map<String, Long> errorDistribution() {
            return errorDistribution;
        }

        public double getSlaRate() {
            return slaRate;
        }

        public double slaRate() {
            return slaRate;
        }

        public double getTimeoutRate() {
            return timeoutRate;
        }

        public double timeoutRate() {
            return timeoutRate;
        }

        public int getConsecutiveFailures() {
            return consecutiveFailures;
        }

        public int consecutiveFailures() {
            return consecutiveFailures;
        }

        public LocalDateTime getLatestCallAt() {
            return latestCallAt;
        }

        public LocalDateTime latestCallAt() {
            return latestCallAt;
        }

        public boolean isLatestCallSuccess() {
            return latestCallSuccess;
        }

        public boolean latestCallSuccess() {
            return latestCallSuccess;
        }
    }

    /**
     * 告警快照。
     */
    @lombok.EqualsAndHashCode
    @lombok.ToString
    final class AlertSnapshot {
        private final String alertType;
        private final String level;
        private final String gatewayId;
        private final String toolName;
        private final String message;
        private final LocalDateTime triggeredAt;

        public AlertSnapshot(String alertType,
                             String level,
                             String gatewayId,
                             String toolName,
                             String message,
                             LocalDateTime triggeredAt) {
            this.alertType = alertType;
            this.level = level;
            this.gatewayId = gatewayId;
            this.toolName = toolName;
            this.message = message;
            this.triggeredAt = triggeredAt;
        }

        public String getAlertType() {
            return alertType;
        }

        public String alertType() {
            return alertType;
        }

        public String getLevel() {
            return level;
        }

        public String level() {
            return level;
        }

        public String getGatewayId() {
            return gatewayId;
        }

        public String gatewayId() {
            return gatewayId;
        }

        public String getToolName() {
            return toolName;
        }

        public String toolName() {
            return toolName;
        }

        public String getMessage() {
            return message;
        }

        public String message() {
            return message;
        }

        public LocalDateTime getTriggeredAt() {
            return triggeredAt;
        }

        public LocalDateTime triggeredAt() {
            return triggeredAt;
        }
    }

    /**
     * 观测报告。
     */
    @lombok.EqualsAndHashCode
    @lombok.ToString
    final class GatewayMetricsReport {
        private final LocalDateTime generatedAt;
        private final int recentMinutes;
        private final List<ToolMetricsSnapshot> toolMetrics;
        private final List<AlertSnapshot> alerts;

        public GatewayMetricsReport(LocalDateTime generatedAt,
                                    int recentMinutes,
                                    List<ToolMetricsSnapshot> toolMetrics,
                                    List<AlertSnapshot> alerts) {
            this.generatedAt = generatedAt;
            this.recentMinutes = recentMinutes;
            this.toolMetrics = toolMetrics;
            this.alerts = alerts;
        }

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }

        public LocalDateTime generatedAt() {
            return generatedAt;
        }

        public int getRecentMinutes() {
            return recentMinutes;
        }

        public int recentMinutes() {
            return recentMinutes;
        }

        public List<ToolMetricsSnapshot> getToolMetrics() {
            return toolMetrics;
        }

        public List<ToolMetricsSnapshot> toolMetrics() {
            return toolMetrics;
        }

        public List<AlertSnapshot> getAlerts() {
            return alerts;
        }

        public List<AlertSnapshot> alerts() {
            return alerts;
        }
    }
}
