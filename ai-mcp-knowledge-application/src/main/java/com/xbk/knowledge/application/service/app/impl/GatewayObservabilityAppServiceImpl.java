package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.GatewayObservabilityAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gateway 可观测性应用服务实现。
 *
 * 在内存维护工具级统计窗口，提供统一指标口径与告警规则。
 *
 * @author sxie
 */
@Slf4j
@Service
public class GatewayObservabilityAppServiceImpl implements GatewayObservabilityAppService {

    /**
     * 默认最近统计窗口（分钟）。
     */
    private static final int DEFAULT_RECENT_MINUTES = 60;

    /**
     * 最近统计窗口最大值（分钟）。
     */
    private static final int MAX_RECENT_MINUTES = 24 * 60;

    /**
     * 指标数据内存保留时长（毫秒）。
     */
    private static final long RETENTION_MILLIS = 24L * 60 * 60 * 1000;

    /**
     * 单工具最多保留事件数。
     */
    private static final int MAX_EVENTS_PER_TOOL = 5000;

    /**
     * 默认超时阈值（毫秒）。
     */
    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    /**
     * 超时突增告警阈值（百分比）。
     */
    private static final double TIMEOUT_SPIKE_RATE = 20.0;

    /**
     * 触发超时突增告警的最小请求量。
     */
    private static final int TIMEOUT_SPIKE_MIN_REQUESTS = 10;

    /**
     * 错误码异常告警阈值（百分比）。
     */
    private static final double ERROR_ANOMALY_RATE = 30.0;

    /**
     * 触发错误码异常告警的最小错误次数。
     */
    private static final int ERROR_ANOMALY_MIN_COUNT = 5;

    /**
     * 连续失败告警阈值。
     */
    private static final int CONSECUTIVE_FAILURE_THRESHOLD = 3;

    /**
     * 告警日志冷却时间（毫秒）。
     */
    private static final long ALERT_LOG_COOLDOWN_MILLIS = 5 * 60 * 1000L;

    /**
     * 默认未知错误码。
     */
    private static final String UNKNOWN_ERROR_CODE = "UNKNOWN";

    /**
     * 告警级别警告。
     */
    private static final String ALERT_LEVEL_WARN = "WARN";

    /**
     * 告警级别严重。
     */
    private static final String ALERT_LEVEL_CRITICAL = "CRITICAL";

    /**
     * 工具指标窗口缓存。
     */
    private final Map<String, ToolMetricWindow> windows = new ConcurrentHashMap<>();

    @Override
    public void recordCall(CallRecord record) {
        if (record == null || !StringUtils.hasText(record.gatewayId()) || !StringUtils.hasText(record.toolName())) {
            return;
        }
        long now = System.currentTimeMillis();
        ToolMetricWindow window = windows.computeIfAbsent(buildKey(record.gatewayId(), record.toolName()),
                key -> new ToolMetricWindow(record.gatewayId(), record.toolName()));
        ToolEvent event = new ToolEvent(
                now,
                sanitizeLatency(record.latencyMs()),
                record.success(),
                normalizeErrorCode(record.errorCode(), record.success()),
                normalizeTimeout(record.timeoutMs())
        );
        window.record(event, now);
    }

    /**
     * 查询网关可观测性。
     *
     * @param query 时间范围查询条件
     * @return 网关指标报告
     */
    @Override
    public GatewayMetricsReport queryMetrics(MetricsQuery query) {
        int recentMinutes = normalizeRecentMinutes(query == null ? null : query.recentMinutes());
        long now = System.currentTimeMillis();
        long cutoff = now - recentMinutes * 60L * 1000L;
        String gatewayId = query == null ? null : query.gatewayId();
        String toolName = query == null ? null : query.toolName();

        List<ToolMetricsSnapshot> metrics = new ArrayList<>();
        List<AlertSnapshot> alerts = new ArrayList<>();

        for (ToolMetricWindow window : windows.values()) {
            if (!match(window.gatewayId, gatewayId) || !match(window.toolName, toolName)) {
                continue;
            }

            WindowSnapshot snapshot = window.snapshot(cutoff, now);
            if (snapshot.totalCount == 0) {
                continue;
            }
            ToolMetricsSnapshot metric = buildMetricsSnapshot(snapshot);
            metrics.add(metric);
            alerts.addAll(buildAlerts(metric, snapshot.maxErrorCode, snapshot.maxErrorCount, now));
        }

        metrics.sort(Comparator
                .comparing(ToolMetricsSnapshot::gatewayId)
                .thenComparing(ToolMetricsSnapshot::toolName));
        alerts.sort(Comparator.comparing(AlertSnapshot::triggeredAt).reversed());

        return new GatewayMetricsReport(LocalDateTime.now(), recentMinutes, metrics, alerts);
    }

    /**
     * 判断是否命中筛选条件。
     *
     * @param value 值
     * @param expected 期望值
     * @return 是否满足筛选条件
     */
    private boolean match(String value, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return Objects.equals(value, expected);
    }

    private ToolMetricsSnapshot buildMetricsSnapshot(WindowSnapshot snapshot) {
        List<Long> latencies = new ArrayList<>(snapshot.latencies);
        latencies.sort(Long::compareTo);
        long p95 = percentile(latencies, 0.95);
        long p99 = percentile(latencies, 0.99);

        double successRate = snapshot.totalCount == 0
                ? 0.0
                : snapshot.successCount * 100.0 / snapshot.totalCount;
        double timeoutRate = snapshot.totalCount == 0
                ? 0.0
                : snapshot.timeoutCount * 100.0 / snapshot.totalCount;
        double slaRate = snapshot.totalCount == 0
                ? 0.0
                : snapshot.slaPassCount * 100.0 / snapshot.totalCount;
        double avgLatency = snapshot.totalCount == 0
                ? 0.0
                : (double) snapshot.totalLatency / snapshot.totalCount;

        return new ToolMetricsSnapshot(
                snapshot.gatewayId,
                snapshot.toolName,
                snapshot.totalCount,
                round(successRate),
                p95,
                p99,
                round(avgLatency),
                snapshot.errorDistribution,
                round(slaRate),
                round(timeoutRate),
                snapshot.consecutiveFailures,
                snapshot.latestCallTimestamp <= 0 ? null : toLocalDateTime(snapshot.latestCallTimestamp),
                snapshot.latestCallSuccess
        );
    }

    /**
     * 构建监控告警列表。
     *
     * @param metric 工具指标快照
     * @param maxErrorCode 最大错误码
     * @param maxErrorCount 最大错误次数
     * @param nowMillis 当前时间戳（毫秒）
     * @return 命中的告警列表
     */
    private List<AlertSnapshot> buildAlerts(ToolMetricsSnapshot metric,
                                            String maxErrorCode,
                                            long maxErrorCount,
                                            long nowMillis) {
        List<AlertSnapshot> alerts = new ArrayList<>();
        if (metric.requestCount() >= TIMEOUT_SPIKE_MIN_REQUESTS && metric.timeoutRate() >= TIMEOUT_SPIKE_RATE) {
            alerts.add(new AlertSnapshot(
                    "TIMEOUT_SPIKE",
                    ALERT_LEVEL_WARN,
                    metric.gatewayId(),
                    metric.toolName(),
                    "超时占比升高，timeoutRate=" + metric.timeoutRate() + "%",
                    toLocalDateTime(nowMillis)
            ));
        }
        if (StringUtils.hasText(maxErrorCode) && maxErrorCount >= ERROR_ANOMALY_MIN_COUNT) {
            double anomalyRate = metric.requestCount() == 0
                    ? 0.0
                    : maxErrorCount * 100.0 / metric.requestCount();
            if (anomalyRate >= ERROR_ANOMALY_RATE) {
                alerts.add(new AlertSnapshot(
                        "ERROR_CODE_ANOMALY",
                        ALERT_LEVEL_WARN,
                        metric.gatewayId(),
                        metric.toolName(),
                        "错误码分布异常，errorCode=" + maxErrorCode + ", rate=" + round(anomalyRate) + "%",
                        toLocalDateTime(nowMillis)
                ));
            }
        }
        if (metric.consecutiveFailures() >= CONSECUTIVE_FAILURE_THRESHOLD) {
            alerts.add(new AlertSnapshot(
                    "CONSECUTIVE_FAILURE",
                    ALERT_LEVEL_CRITICAL,
                    metric.gatewayId(),
                    metric.toolName(),
                    "连续失败次数达到 " + metric.consecutiveFailures(),
                    toLocalDateTime(nowMillis)
            ));
        }
        return alerts;
    }

    /**
     * 计算延迟分位值。
     *
     * @param sortedLatencies 排序后的延迟列表
     * @param percentile 分位点
     * @return 指定分位点对应的延迟值
     */
    private long percentile(List<Long> sortedLatencies, double percentile) {
        if (sortedLatencies == null || sortedLatencies.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(percentile * sortedLatencies.size()) - 1;
        index = Math.max(0, Math.min(index, sortedLatencies.size() - 1));
        return sortedLatencies.get(index);
    }

    /**
     * 归一化错误编码。
     *
     * @param errorCode 错误码
     * @param success 调用是否成功
     * @return 归一化后的错误编码
     */
    private String normalizeErrorCode(String errorCode, boolean success) {
        if (success) {
            return "";
        }
        if (!StringUtils.hasText(errorCode)) {
            return UNKNOWN_ERROR_CODE;
        }
        return errorCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化延迟值。
     *
     * @param latencyMs 延迟（毫秒）
     * @return 清洗后的延迟值（毫秒）
     */
    private long sanitizeLatency(long latencyMs) {
        if (latencyMs < 0) {
            return 0L;
        }
        return latencyMs;
    }

    /**
     * 归一化超时。
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 归一化后的超时时间（毫秒）
     */
    private int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null || timeoutMs <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return timeoutMs;
    }

    /**
     * 归一化最近时间窗口。
     *
     * @param recentMinutes 最近时间窗口（分钟）
     * @return 归一化后的时间窗口（分钟）
     */
    private int normalizeRecentMinutes(Integer recentMinutes) {
        if (recentMinutes == null || recentMinutes <= 0) {
            return DEFAULT_RECENT_MINUTES;
        }
        return Math.min(recentMinutes, MAX_RECENT_MINUTES);
    }

    private String buildKey(String gatewayId, String toolName) {
        return gatewayId + "||" + toolName;
    }

    private LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private class ToolMetricWindow {

        /**
         * 网关标识。
         */
        private final String gatewayId;

        /**
         * 工具名称。
         */
        private final String toolName;

        /**
         * 工具事件队列。
         */
        private final Deque<ToolEvent> events = new ArrayDeque<>();

        /**
         * 连续失败次数。
         */
        private int consecutiveFailures;

        /**
         * 告警类型到最近日志时间的映射。
         */
        private final Map<String, AtomicLong> alertLastLogTime = new ConcurrentHashMap<>();

        private ToolMetricWindow(String gatewayId, String toolName) {
            this.gatewayId = gatewayId;
            this.toolName = toolName;
        }

        private synchronized void record(ToolEvent event, long now) {
            events.addLast(event);
            if (event.success) {
                consecutiveFailures = 0;
            } else {
                consecutiveFailures++;
            }
            trimExpired(now);
            emitAlertLogsIfNeeded(now);
        }

        private synchronized WindowSnapshot snapshot(long cutoff, long now) {
            trimExpired(now);
            WindowSnapshot snapshot = new WindowSnapshot(gatewayId, toolName, consecutiveFailures);
            for (ToolEvent event : events) {
                if (event.timestamp < cutoff) {
                    continue;
                }
                snapshot.totalCount++;
                snapshot.latestCallTimestamp = event.timestamp;
                snapshot.latestCallSuccess = event.success;
                snapshot.totalLatency += event.latency;
                snapshot.latencies.add(event.latency);
                if (event.success) {
                    snapshot.successCount++;
                } else {
                    snapshot.errorDistribution.merge(event.errorCode, 1L, Long::sum);
                }
                if (event.latency > event.timeoutMs) {
                    snapshot.timeoutCount++;
                }
                if (event.success && event.latency <= event.timeoutMs) {
                    snapshot.slaPassCount++;
                }
            }
            for (Map.Entry<String, Long> entry : snapshot.errorDistribution.entrySet()) {
                if (entry.getValue() > snapshot.maxErrorCount) {
                    snapshot.maxErrorCount = entry.getValue();
                    snapshot.maxErrorCode = entry.getKey();
                }
            }
            snapshot.errorDistribution = sortByValueDesc(snapshot.errorDistribution);
            return snapshot;
        }

        private void emitAlertLogsIfNeeded(long now) {
            WindowSnapshot snapshot = snapshot(now - 5 * 60 * 1000L, now);
            ToolMetricsSnapshot metric = buildMetricsSnapshot(snapshot);
            List<AlertSnapshot> alerts = buildAlerts(metric, snapshot.maxErrorCode, snapshot.maxErrorCount, now);
            for (AlertSnapshot alert : alerts) {
                AtomicLong lastTime = alertLastLogTime.computeIfAbsent(alert.alertType(), key -> new AtomicLong(0L));
                long prev = lastTime.get();
                if (now - prev < ALERT_LOG_COOLDOWN_MILLIS) {
                    continue;
                }
                if (lastTime.compareAndSet(prev, now)) {
                    log.warn("Gateway 告警触发 type: {}, level: {}, gatewayId: {}, toolName: {}, message: {}",
                            alert.alertType(), alert.level(), alert.gatewayId(), alert.toolName(), alert.message());
                }
            }
        }

        private void trimExpired(long now) {
            long minTime = now - RETENTION_MILLIS;
            while (!events.isEmpty() && (events.peekFirst().timestamp < minTime || events.size() > MAX_EVENTS_PER_TOOL)) {
                events.removeFirst();
            }
        }
    }

    private Map<String, Long> sortByValueDesc(Map<String, Long> input) {
        Map<String, Long> sorted = new LinkedHashMap<>();
        input.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    private static class ToolEvent {

        /**
         * 事件时间戳（毫秒）。
         */
        private final long timestamp;

        /**
         * 调用耗时（毫秒）。
         */
        private final long latency;

        /**
         * 是否成功。
         */
        private final boolean success;

        /**
         * 错误码。
         */
        private final String errorCode;

        /**
         * 超时阈值（毫秒）。
         */
        private final int timeoutMs;

        private ToolEvent(long timestamp, long latency, boolean success, String errorCode, int timeoutMs) {
            this.timestamp = timestamp;
            this.latency = latency;
            this.success = success;
            this.errorCode = errorCode;
            this.timeoutMs = timeoutMs;
        }
    }

    private static class WindowSnapshot {

        /**
         * 网关标识。
         */
        private final String gatewayId;

        /**
         * 工具名称。
         */
        private final String toolName;

        /**
         * 连续失败次数。
         */
        private final int consecutiveFailures;

        /**
         * 请求总数。
         */
        private long totalCount;

        /**
         * 成功请求数。
         */
        private long successCount;

        /**
         * 超时请求数。
         */
        private long timeoutCount;

        /**
         * SLA 达标请求数。
         */
        private long slaPassCount;

        /**
         * 总耗时（毫秒）。
         */
        private long totalLatency;

        /**
         * 延迟采样列表。
         */
        private List<Long> latencies = new ArrayList<>();

        /**
         * 错误码分布统计。
         */
        private Map<String, Long> errorDistribution = new HashMap<>();

        /**
         * 最大错误码计数。
         */
        private long maxErrorCount;

        /**
         * 错误数最高的错误码。
         */
        private String maxErrorCode;

        /**
         * 最近一次调用时间戳（毫秒）。
         */
        private long latestCallTimestamp;

        /**
         * 最近一次调用是否成功。
         */
        private boolean latestCallSuccess;

        private WindowSnapshot(String gatewayId, String toolName, int consecutiveFailures) {
            this.gatewayId = gatewayId;
            this.toolName = toolName;
            this.consecutiveFailures = consecutiveFailures;
        }
    }
}
