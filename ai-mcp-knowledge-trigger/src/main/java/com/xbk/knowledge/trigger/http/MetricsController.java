package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.IMetricsService;
import com.xbk.knowledge.api.dto.metrics.CallMetricsDTO;
import com.xbk.knowledge.api.dto.metrics.MetricsQueryRequest;
import com.xbk.knowledge.api.dto.metrics.ModelUsageDTO;
import com.xbk.knowledge.api.dto.metrics.ModelUsageQueryRequest;
import com.xbk.knowledge.api.dto.metrics.ResponseTimeDTO;
import com.xbk.knowledge.api.dto.metrics.SuccessRateDTO;
import com.xbk.knowledge.domain.metrics.model.valobj.CallMetrics;
import com.xbk.knowledge.domain.metrics.model.valobj.MetricsQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsage;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsageQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ResponseTime;
import com.xbk.knowledge.domain.metrics.model.valobj.SuccessRate;
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
 *
 * @author sxie
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
         *
         * 为什么：统一统计入口，避免调用方直接访问领域对象。
         * 流程：
         * 1. 进入接口后执行 `audit:read` 权限校验。
         * 2. Spring 完成请求参数绑定（当前接口未开启 `@Valid` 强校验）。
         * 3. Controller 组装 `MetricsQuery` 并调用 `metricsAppService.collectCallMetrics`。
         * 4. 将领域统计对象转换为 `CallMetricsDTO`。
         * 5. 统一封装 `Result.success` 返回。
         *
         * @param request 统计查询参数
         * @return 调用量统计结果
         */
        @Override
        @PostMapping("/calls")
        @SaCheckPermission("audit:read")
        public Result<CallMetricsDTO> getCallMetrics(@RequestBody MetricsQueryRequest request) {
                // 调用应用服务收集指标
                Long modelId = request.getModelId();
                LocalDateTime startTime = request.getStartTime();
                LocalDateTime endTime = request.getEndTime();
                MetricsQuery query = new MetricsQuery(
                                modelId,
                                startTime,
                                endTime);
                CallMetrics metrics = metricsAppService.collectCallMetrics(query);

                // 转换为 API DTO
                Long totalCalls = metrics.getTotalCalls();
                Long successCalls = metrics.getSuccessCalls();
                Long failedCalls = metrics.getFailedCalls();
                CallMetricsDTO dto = new CallMetricsDTO(
                                totalCalls,
                                successCalls,
                                failedCalls,
                                0L // fallbackCalls - CallMetrics 中没有这个字段，暂时用 0
                );

                return Result.success(dto);
        }

        /**
         * 对外暴露 getSuccessRate 作为调用入口，便于上层复用。
         *
         * 为什么：成功率计算口径集中管理，避免多处重复计算。
         * 流程：
         * 1. 进入接口后执行 `audit:read` 权限校验。
         * 2. Spring 完成请求参数绑定。
         * 3. Controller 组装 `MetricsQuery` 并调用 `metricsAppService.collectSuccessRate`。
         * 4. 将领域统计对象转换为 `SuccessRateDTO`。
         * 5. 统一封装 `Result.success` 返回。
         *
         * @param request 统计查询参数
         * @return 成功率统计结果
         */
        @Override
        @PostMapping("/success-rate")
        @SaCheckPermission("audit:read")
        public Result<SuccessRateDTO> getSuccessRate(@RequestBody MetricsQueryRequest request) {
                // 调用应用服务收集指标
                Long modelId = request.getModelId();
                LocalDateTime startTime = request.getStartTime();
                LocalDateTime endTime = request.getEndTime();
                MetricsQuery query = new MetricsQuery(
                                modelId,
                                startTime,
                                endTime);
                SuccessRate successRate = metricsAppService.collectSuccessRate(query);

                // 转换为 API DTO
                Long totalCalls = successRate.getTotalCalls();
                Long successCalls = successRate.getSuccessCalls();
                Double successRatio = successRate.getSuccessRate();
                SuccessRateDTO dto = new SuccessRateDTO(
                                totalCalls,
                                successCalls,
                                successRatio);

                return Result.success(dto);
        }

        /**
         * 对外暴露 getResponseTime 作为调用入口，便于上层复用。
         *
         * 为什么：响应耗时口径统一，便于趋势分析。
         * 流程：
         * 1. 进入接口后执行 `audit:read` 权限校验。
         * 2. Spring 完成请求参数绑定。
         * 3. Controller 组装 `MetricsQuery` 并调用 `metricsAppService.collectResponseTime`。
         * 4. 将领域统计对象转换为 `ResponseTimeDTO`。
         * 5. 统一封装 `Result.success` 返回。
         *
         * @param request 统计查询参数
         * @return 响应耗时统计结果
         */
        @Override
        @PostMapping("/response-time")
        @SaCheckPermission("audit:read")
        public Result<ResponseTimeDTO> getResponseTime(@RequestBody MetricsQueryRequest request) {
                // 调用应用服务收集指标
                Long modelId = request.getModelId();
                LocalDateTime startTime = request.getStartTime();
                LocalDateTime endTime = request.getEndTime();
                MetricsQuery query = new MetricsQuery(
                                modelId,
                                startTime,
                                endTime);
                ResponseTime responseTime = metricsAppService.collectResponseTime(query);

                // 转换为 API DTO
                Double avgResponseTime = responseTime.getAvgResponseTime();
                Long maxResponseTime = responseTime.getMaxResponseTime();
                Long minResponseTime = responseTime.getMinResponseTime();
                ResponseTimeDTO dto = new ResponseTimeDTO(
                                avgResponseTime,
                                maxResponseTime,
                                minResponseTime);

                return Result.success(dto);
        }

        /**
         * 对外暴露 getModelUsage 作为调用入口，便于上层复用。
         *
         * 为什么：统一模型使用分布计算，避免各端自行统计。
         * 流程：
         * 1. 进入接口后执行 `audit:read` 权限校验。
         * 2. Spring 完成请求参数绑定。
         * 3. Controller 组装 `ModelUsageQuery` 并调用 `metricsAppService.collectModelUsage`。
         * 4. 计算总调用量后映射为 `ModelUsageDTO`（含占比）。
         * 5. 统一封装 `Result.success` 返回。
         *
         * @param request 模型使用统计查询参数
         * @return 模型使用分布结果
         */
        @Override
        @PostMapping("/model-usage")
        @SaCheckPermission("audit:read")
        public Result<List<ModelUsageDTO>> getModelUsage(@RequestBody ModelUsageQueryRequest request) {
                // 调用应用服务收集指标
                LocalDateTime startTime = request.getStartTime();
                LocalDateTime endTime = request.getEndTime();
                ModelUsageQuery query = new ModelUsageQuery(
                                startTime,
                                endTime);
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
                                        usageRate);
                };
                List<ModelUsageDTO> dtoList = usageList
                                .stream()
                                .map(usageMapper)
                                .collect(toListCollector);

                return Result.success(dtoList);
        }
}
