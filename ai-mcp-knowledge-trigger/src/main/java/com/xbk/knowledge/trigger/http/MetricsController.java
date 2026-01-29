package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.IMetricsService;
import com.xbk.knowledge.api.dto.metrics.CallMetricsDTO;
import com.xbk.knowledge.api.dto.metrics.MetricsQueryRequest;
import com.xbk.knowledge.api.dto.metrics.ModelUsageDTO;
import com.xbk.knowledge.api.dto.metrics.ModelUsageQueryRequest;
import com.xbk.knowledge.api.dto.metrics.ResponseTimeDTO;
import com.xbk.knowledge.api.dto.metrics.SuccessRateDTO;
import com.xbk.knowledge.domain.model.vo.CallMetrics;
import com.xbk.knowledge.domain.model.vo.ModelUsage;
import com.xbk.knowledge.domain.model.vo.ResponseTime;
import com.xbk.knowledge.domain.model.vo.SuccessRate;
import com.xbk.knowledge.domain.service.IMetricsDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控统计 Controller
 * 负责接收 HTTP 请求，调用领域服务，转换响应
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

    private final IMetricsDomainService metricsDomainService;

    @Override
    @PostMapping("/calls")
    public Result<CallMetricsDTO> getCallMetrics(@Valid @RequestBody MetricsQueryRequest request) {
        // 调用领域服务收集指标
        CallMetrics metrics = metricsDomainService.collectCallMetrics(
                request.getModelId(),
                request.getTaskType(),
                request.getStartTime(),
                request.getEndTime()
        );

        // 转换为 API DTO
        CallMetricsDTO dto = new CallMetricsDTO(
                metrics.getTotalCalls(),
                metrics.getSuccessCalls(),
                metrics.getFailedCalls(),
                0L  // fallbackCalls - CallMetrics 中没有这个字段，暂时用 0
        );

        return Result.success(dto);
    }

    @Override
    @PostMapping("/success-rate")
    public Result<SuccessRateDTO> getSuccessRate(@Valid @RequestBody MetricsQueryRequest request) {
        // 调用领域服务收集指标
        SuccessRate successRate = metricsDomainService.collectSuccessRate(
                request.getModelId(),
                request.getTaskType(),
                request.getStartTime(),
                request.getEndTime()
        );

        // 转换为 API DTO
        SuccessRateDTO dto = new SuccessRateDTO(
                successRate.getTotalCalls(),
                successRate.getSuccessCalls(),
                successRate.getSuccessRate()
        );

        return Result.success(dto);
    }

    @Override
    @PostMapping("/response-time")
    public Result<ResponseTimeDTO> getResponseTime(@Valid @RequestBody MetricsQueryRequest request) {
        // 调用领域服务收集指标
        ResponseTime responseTime = metricsDomainService.collectResponseTime(
                request.getModelId(),
                request.getTaskType(),
                request.getStartTime(),
                request.getEndTime()
        );

        // 转换为 API DTO
        ResponseTimeDTO dto = new ResponseTimeDTO(
                responseTime.getAvgResponseTime(),
                responseTime.getMaxResponseTime(),
                responseTime.getMinResponseTime()
        );

        return Result.success(dto);
    }

    @Override
    @PostMapping("/model-usage")
    public Result<List<ModelUsageDTO>> getModelUsage(@Valid @RequestBody ModelUsageQueryRequest request) {
        // 调用领域服务收集指标
        List<ModelUsage> usageList = metricsDomainService.collectModelUsage(
                request.getStartTime(),
                request.getEndTime()
        );

        // 计算总调用次数
        long totalCalls = usageList.stream()
                .mapToLong(ModelUsage::getCallCount)
                .sum();

        // 转换为 API DTO
        List<ModelUsageDTO> dtoList = usageList.stream()
                .map(usage -> {
                    double usageRate = totalCalls > 0
                            ? (usage.getCallCount() * 100.0 / totalCalls)
                            : 0.0;
                    return new ModelUsageDTO(
                            usage.getModelId(),
                            usage.getCallCount(),
                            usageRate
                    );
                })
                .collect(Collectors.toList());

        return Result.success(dtoList);
    }
}
