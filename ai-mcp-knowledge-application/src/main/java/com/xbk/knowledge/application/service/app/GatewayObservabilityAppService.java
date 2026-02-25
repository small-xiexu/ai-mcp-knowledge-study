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
     * 调用记录
     *
     * @param gatewayId 网关ID
     * @param toolName 工具名
     * @param success 是否成功
     * @param errorCode 错误码
     * @param latencyMs 调用耗时（毫秒）
     * @param timeoutMs 超时阈值（毫秒）
     */
    record CallRecord(String gatewayId,
                      String toolName,
                      boolean success,
                      String errorCode,
                      long latencyMs,
                      Integer timeoutMs) {
    }

    /**
     * 指标查询条件
     *
     * @param gatewayId 网关ID（可选）
     * @param toolName 工具名（可选）
     * @param recentMinutes 查询窗口分钟数（可选）
     */
    record MetricsQuery(String gatewayId, String toolName, Integer recentMinutes) {
    }

    /**
     * 工具指标快照
     *
     * @param gatewayId 网关ID
     * @param toolName 工具名
     * @param requestCount 请求量
     * @param successRate 成功率（百分比）
     * @param p95LatencyMs P95延迟
     * @param p99LatencyMs P99延迟
     * @param avgLatencyMs 平均延迟
     * @param errorDistribution 错误分布
     * @param slaRate 工具级SLA达标率（百分比）
     * @param timeoutRate 超时占比（百分比）
     * @param consecutiveFailures 连续失败次数
     */
    record ToolMetricsSnapshot(String gatewayId,
                               String toolName,
                               long requestCount,
                               double successRate,
                               long p95LatencyMs,
                               long p99LatencyMs,
                               double avgLatencyMs,
                               Map<String, Long> errorDistribution,
                               double slaRate,
                               double timeoutRate,
                               int consecutiveFailures) {
    }

    /**
     * 告警快照
     *
     * @param alertType 告警类型
     * @param level 告警等级
     * @param gatewayId 网关ID
     * @param toolName 工具名
     * @param message 告警内容
     * @param triggeredAt 触发时间
     */
    record AlertSnapshot(String alertType,
                         String level,
                         String gatewayId,
                         String toolName,
                         String message,
                         LocalDateTime triggeredAt) {
    }

    /**
     * 观测报告
     *
     * @param generatedAt 生成时间
     * @param recentMinutes 查询窗口分钟数
     * @param toolMetrics 工具指标
     * @param alerts 告警列表
     */
    record GatewayMetricsReport(LocalDateTime generatedAt,
                                int recentMinutes,
                                List<ToolMetricsSnapshot> toolMetrics,
                                List<AlertSnapshot> alerts) {
    }
}
