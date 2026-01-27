package com.xbk.knowledge.orchestration.controller;

import com.xbk.knowledge.orchestration.controller.common.Result;
import com.xbk.knowledge.orchestration.model.dto.CallMetricsDTO;
import com.xbk.knowledge.orchestration.model.dto.ModelUsageDTO;
import com.xbk.knowledge.orchestration.model.dto.ResponseTimeDTO;
import com.xbk.knowledge.orchestration.model.dto.SuccessRateDTO;
import com.xbk.knowledge.orchestration.service.MetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 监控统计 Controller
 * 提供 AI 调用监控的统计接口
 *
 * @author xiexu
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsCollector metricsCollector;

    /**
     * 调用次数统计
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 调用次数统计
     */
    @GetMapping("/calls")
    public Result<CallMetricsDTO> getCallMetrics(@RequestParam(required = false) @Min(1) Long modelId,
                                                 @RequestParam(required = false) String taskType,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                 LocalDateTime startTime,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                 LocalDateTime endTime) {
        log.info("查询调用次数统计，modelId: {}, taskType: {}, startTime: {}, endTime: {}",
                modelId, taskType, startTime, endTime);

        var metrics = metricsCollector.collectCallMetrics(modelId, taskType, startTime, endTime);
        return Result.success(metrics);
    }

    /**
     * 成功率统计
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 成功率统计
     */
    @GetMapping("/success-rate")
    public Result<SuccessRateDTO> getSuccessRate(@RequestParam(required = false) @Min(1) Long modelId,
                                                 @RequestParam(required = false) String taskType,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                 LocalDateTime startTime,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                 LocalDateTime endTime) {
        log.info("查询成功率统计，modelId: {}, taskType: {}, startTime: {}, endTime: {}",
                modelId, taskType, startTime, endTime);

        var metrics = metricsCollector.collectSuccessRate(modelId, taskType, startTime, endTime);
        return Result.success(metrics);
    }

    /**
     * 响应时间统计
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 响应时间统计
     */
    @GetMapping("/response-time")
    public Result<ResponseTimeDTO> getResponseTime(@RequestParam(required = false) @Min(1) Long modelId,
                                                   @RequestParam(required = false) String taskType,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                   LocalDateTime startTime,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                   LocalDateTime endTime) {
        log.info("查询响应时间统计，modelId: {}, taskType: {}, startTime: {}, endTime: {}",
                modelId, taskType, startTime, endTime);

        var metrics = metricsCollector.collectResponseTime(modelId, taskType, startTime, endTime);
        return Result.success(metrics);
    }

    /**
     * 模型使用分布
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 模型使用分布
     */
    @GetMapping("/model-usage")
    public Result<List<ModelUsageDTO>> getModelUsage(@RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                     LocalDateTime startTime,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                     LocalDateTime endTime) {
        log.info("查询模型使用分布，startTime: {}, endTime: {}", startTime, endTime);

        var metrics = metricsCollector.collectModelUsage(startTime, endTime);
        return Result.success(metrics);
    }
}
