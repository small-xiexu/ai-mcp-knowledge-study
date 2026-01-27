package com.xbk.knowledge.orchestration.service;

import com.xbk.knowledge.orchestration.domain.repository.CallLogRepository;
import com.xbk.knowledge.orchestration.model.dto.CallMetricsDTO;
import com.xbk.knowledge.orchestration.model.dto.ModelUsageDTO;
import com.xbk.knowledge.orchestration.model.dto.ResponseTimeDTO;
import com.xbk.knowledge.orchestration.model.dto.SuccessRateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控指标收集器
 * 统一封装调用统计的聚合逻辑，避免控制器直接访问仓储
 *
 * @author xiexu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsCollector {

    private final CallLogRepository callLogRepository;

    /**
     * 统计调用次数指标
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 调用次数统计
     */
    public CallMetricsDTO collectCallMetrics(Long modelId, String taskType,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        log.info("开始统计调用次数，modelId: {}, taskType: {}, startTime: {}, endTime: {}",
                modelId, taskType, startTime, endTime);

        var metrics = callLogRepository.aggregateCallMetrics(modelId, taskType, startTime, endTime);
        var normalized = normalizeCallMetrics(metrics);

        log.info("调用次数统计完成，total: {}, success: {}, failed: {}, fallback: {}",
                normalized.totalCalls(), normalized.successCalls(), normalized.failedCalls(), normalized.fallbackCalls());
        return normalized;
    }

    /**
     * 统计成功率指标
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 成功率统计
     */
    public SuccessRateDTO collectSuccessRate(Long modelId, String taskType,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        log.info("开始统计成功率，modelId: {}, taskType: {}, startTime: {}, endTime: {}",
                modelId, taskType, startTime, endTime);

        var successRate = callLogRepository.aggregateSuccessRate(modelId, taskType, startTime, endTime);
        var normalized = normalizeSuccessRate(successRate);

        log.info("成功率统计完成，total: {}, success: {}, rate: {}",
                normalized.totalCalls(), normalized.successCalls(), normalized.successRate());
        return normalized;
    }

    /**
     * 统计响应时间指标
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 响应时间统计
     */
    public ResponseTimeDTO collectResponseTime(Long modelId, String taskType,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        log.info("开始统计响应时间，modelId: {}, taskType: {}, startTime: {}, endTime: {}",
                modelId, taskType, startTime, endTime);

        var responseTime = callLogRepository.aggregateResponseTime(modelId, taskType, startTime, endTime);
        var normalized = normalizeResponseTime(responseTime);

        log.info("响应时间统计完成，avg: {}, max: {}, min: {}",
                normalized.avgResponseTime(), normalized.maxResponseTime(), normalized.minResponseTime());
        return normalized;
    }

    /**
     * 统计模型使用分布
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 模型使用分布
     */
    public List<ModelUsageDTO> collectModelUsage(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        log.info("开始统计模型使用分布，startTime: {}, endTime: {}", startTime, endTime);

        var usageList = callLogRepository.aggregateModelUsage(startTime, endTime);
        if (usageList == null || usageList.isEmpty()) {
            log.info("模型使用分布为空");
            return Collections.emptyList();
        }

        var totalCalls = usageList.stream()
                .map(ModelUsageDTO::callCount)
                .mapToLong(value -> value != null ? value : 0L)
                .sum();

        var normalized = usageList.stream()
                .map(item -> {
                    var callCount = defaultCount(item.callCount());
                    var usageRate = totalCalls == 0 ? 0.0 : callCount * 100.0 / totalCalls;
                    return new ModelUsageDTO(item.modelId(), callCount, usageRate);
                })
                .collect(Collectors.toList());

        log.info("模型使用分布统计完成，modelCount: {}", normalized.size());
        return normalized;
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime 不能晚于 endTime");
        }
    }

    private CallMetricsDTO normalizeCallMetrics(CallMetricsDTO metrics) {
        if (metrics == null) {
            return new CallMetricsDTO(0L, 0L, 0L, 0L);
        }
        var totalCalls = defaultCount(metrics.totalCalls());
        var successCalls = defaultCount(metrics.successCalls());
        var failedCalls = defaultCount(metrics.failedCalls());
        var fallbackCalls = defaultCount(metrics.fallbackCalls());
        return new CallMetricsDTO(totalCalls, successCalls, failedCalls, fallbackCalls);
    }

    private SuccessRateDTO normalizeSuccessRate(SuccessRateDTO successRate) {
        if (successRate == null) {
            return new SuccessRateDTO(0L, 0L, 0.0);
        }
        var totalCalls = defaultCount(successRate.totalCalls());
        var successCalls = defaultCount(successRate.successCalls());
        var rate = totalCalls == 0 ? 0.0 : successCalls * 100.0 / totalCalls;
        return new SuccessRateDTO(totalCalls, successCalls, rate);
    }

    private ResponseTimeDTO normalizeResponseTime(ResponseTimeDTO responseTime) {
        if (responseTime == null) {
            return new ResponseTimeDTO(0.0, 0L, 0L);
        }
        var avgTime = responseTime.avgResponseTime() != null ? responseTime.avgResponseTime() : 0.0;
        var maxTime = responseTime.maxResponseTime() != null ? responseTime.maxResponseTime() : 0L;
        var minTime = responseTime.minResponseTime() != null ? responseTime.minResponseTime() : 0L;
        return new ResponseTimeDTO(avgTime, maxTime, minTime);
    }

    private Long defaultCount(Long value) {
        return value != null ? value : 0L;
    }
}
