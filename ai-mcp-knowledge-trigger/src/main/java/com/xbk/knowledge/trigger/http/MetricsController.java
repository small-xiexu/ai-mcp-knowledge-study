package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.IMetricsService;
import com.xbk.knowledge.api.dto.metrics.CallMetricsDTO;
import com.xbk.knowledge.api.dto.metrics.MetricsQueryRequest;
import com.xbk.knowledge.api.dto.metrics.ModelUsageDTO;
import com.xbk.knowledge.api.dto.metrics.ModelUsageQueryRequest;
import com.xbk.knowledge.api.dto.metrics.ResponseTimeDTO;
import com.xbk.knowledge.api.dto.metrics.SuccessRateDTO;
import com.xbk.knowledge.domain.model.vo.metrics.CallMetrics;
import com.xbk.knowledge.domain.model.vo.metrics.MetricsQuery;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsage;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsageQuery;
import com.xbk.knowledge.domain.model.vo.metrics.ResponseTime;
import com.xbk.knowledge.domain.model.vo.metrics.SuccessRate;
import com.xbk.knowledge.application.service.app.MetricsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * 监控统计 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author xiexu
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController implements IMetricsService {

    private final MetricsAppService metricsAppService;

    /**
     * 对外暴露 getCallMetrics 作为调用入口，便于上层复用。
     */
    @Override
    @PostMapping("/calls")
    public Result<CallMetricsDTO> getCallMetrics(@Valid @RequestBody MetricsQueryRequest request) {
        // 调用应用服务收集指标
        Long modelId = request.getModelId();
        String taskType = request.getTaskType();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        MetricsQuery query = new MetricsQuery(
                modelId,
                taskType,
                startTime,
                endTime
        );
        CallMetrics metrics = metricsAppService.collectCallMetrics(query);

        // 转换为 API DTO
        Long totalCalls = metrics.getTotalCalls();
        Long successCalls = metrics.getSuccessCalls();
        Long failedCalls = metrics.getFailedCalls();
        CallMetricsDTO dto = new CallMetricsDTO(
                totalCalls,
                successCalls,
                failedCalls,
                0L  // fallbackCalls - CallMetrics 中没有这个字段，暂时用 0
        );

        return Result.success(dto);
    }

    /**
     * 对外暴露 getSuccessRate 作为调用入口，便于上层复用。
     */
    @Override
    @PostMapping("/success-rate")
    public Result<SuccessRateDTO> getSuccessRate(@Valid @RequestBody MetricsQueryRequest request) {
        // 调用应用服务收集指标
        Long modelId = request.getModelId();
        String taskType = request.getTaskType();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        MetricsQuery query = new MetricsQuery(
                modelId,
                taskType,
                startTime,
                endTime
        );
        SuccessRate successRate = metricsAppService.collectSuccessRate(query);

        // 转换为 API DTO
        Long totalCalls = successRate.getTotalCalls();
        Long successCalls = successRate.getSuccessCalls();
        Double successRatio = successRate.getSuccessRate();
        SuccessRateDTO dto = new SuccessRateDTO(
                totalCalls,
                successCalls,
                successRatio
        );

        return Result.success(dto);
    }

    /**
     * 对外暴露 getResponseTime 作为调用入口，便于上层复用。
     */
    @Override
    @PostMapping("/response-time")
    public Result<ResponseTimeDTO> getResponseTime(@Valid @RequestBody MetricsQueryRequest request) {
        // 调用应用服务收集指标
        Long modelId = request.getModelId();
        String taskType = request.getTaskType();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        MetricsQuery query = new MetricsQuery(
                modelId,
                taskType,
                startTime,
                endTime
        );
        ResponseTime responseTime = metricsAppService.collectResponseTime(query);

        // 转换为 API DTO
        Double avgResponseTime = responseTime.getAvgResponseTime();
        Long maxResponseTime = responseTime.getMaxResponseTime();
        Long minResponseTime = responseTime.getMinResponseTime();
        ResponseTimeDTO dto = new ResponseTimeDTO(
                avgResponseTime,
                maxResponseTime,
                minResponseTime
        );

        return Result.success(dto);
    }

    /**
     * 对外暴露 getModelUsage 作为调用入口，便于上层复用。
     */
    @Override
    @PostMapping("/model-usage")
    public Result<List<ModelUsageDTO>> getModelUsage(@Valid @RequestBody ModelUsageQueryRequest request) {
        // 调用应用服务收集指标
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        ModelUsageQuery query = new ModelUsageQuery(
                startTime,
                endTime
        );
        List<ModelUsage> usageList = metricsAppService.collectModelUsage(query);

        // 计算总调用次数
        ToLongFunction<ModelUsage> callCountMapper = ModelUsage::getCallCount;
        long totalCalls = usageList
                .stream()
                .mapToLong(callCountMapper)
                .sum();

        // 转换为 API DTO
        Collector<ModelUsageDTO, ?, List<ModelUsageDTO>> toListCollector = Collectors.toList();
        Function<ModelUsage, ModelUsageDTO> usageMapper = usage -> {
            Long callCount = usage.getCallCount();
            double usageRate = totalCalls > 0
                    ? (callCount * 100.0 / totalCalls)
                    : 0.0;
            Long modelId = usage.getModelId();
            return new ModelUsageDTO(
                    modelId,
                    callCount,
                    usageRate
            );
        };
        List<ModelUsageDTO> dtoList = usageList
                .stream()
                .map(usageMapper)
                .collect(toListCollector);

        return Result.success(dtoList);
    }
}
