package com.xbk.knowledge.domain.metrics.service.impl;

import com.xbk.knowledge.domain.metrics.model.valobj.CallMetrics;
import com.xbk.knowledge.domain.metrics.model.valobj.MetricsQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsage;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsageQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ResponseTime;
import com.xbk.knowledge.domain.metrics.model.valobj.SuccessRate;
import com.xbk.knowledge.domain.metrics.adapter.repository.CallLogRepository;
import com.xbk.knowledge.domain.metrics.service.IMetricsDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 监控指标领域服务实现
 * 封装监控指标的业务逻辑
 *
 * 职责：领域服务实现，用于封装业务规则
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsDomainServiceImpl implements IMetricsDomainService {

    private final CallLogRepository callLogRepository;

    /**
     * 统计调用次数指标
     *
     * 为什么：统一时间范围校验并规范化返回结构
     * 入参：指标查询对象
     * 出参：调用次数指标
     */
    @Override
    public CallMetrics collectCallMetrics(MetricsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("指标查询条件不能为空");
        }
        // 校验时间范围，避免无效查询
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        validateTimeRange(startTime, endTime);

        // 调用仓储完成指标聚合
        CallMetrics metrics = callLogRepository.aggregateCallMetrics(query);

        // 规范化输出，避免前端空指针
        return normalizeCallMetrics(metrics);
    }

    /**
     * 统计成功率指标
     *
     * 为什么：统一时间范围校验并规范化成功率口径
     * 入参：指标查询对象
     * 出参：成功率指标
     */
    @Override
    public SuccessRate collectSuccessRate(MetricsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("指标查询条件不能为空");
        }
        // 校验时间范围，避免无效查询
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        validateTimeRange(startTime, endTime);

        // 调用仓储完成指标聚合
        SuccessRate successRate = callLogRepository.aggregateSuccessRate(query);

        // 规范化输出，保证成功率口径统一
        return normalizeSuccessRate(successRate);
    }

    /**
     * 统计响应时间指标
     *
     * 为什么：统一时间范围校验并规范化响应时间数据
     * 入参：指标查询对象
     * 出参：响应时间指标
     */
    @Override
    public ResponseTime collectResponseTime(MetricsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("指标查询条件不能为空");
        }
        // 校验时间范围，避免无效查询
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        validateTimeRange(startTime, endTime);

        // 调用仓储完成指标聚合
        ResponseTime responseTime = callLogRepository.aggregateResponseTime(query);

        // 规范化输出，避免 null 导致图表渲染失败
        return normalizeResponseTime(responseTime);
    }

    /**
     * 统计模型使用分布
     *
     * 为什么：统一时间范围校验并保证返回列表稳定
     * 入参：模型使用查询对象
     * 出参：模型使用分布列表
     */
    @Override
    public List<ModelUsage> collectModelUsage(ModelUsageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("模型使用查询条件不能为空");
        }
        // 校验时间范围，避免无效查询
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        validateTimeRange(startTime, endTime);

        // 调用仓储完成指标聚合
        List<ModelUsage> usageList = callLogRepository.aggregateModelUsage(query);

        // 保证返回稳定结构，避免空指针
        return usageList != null ? usageList : Collections.emptyList();
    }

    /**
     * 验证时间范围
     *
     * 为什么：防止开始时间晚于结束时间导致统计无意义
     * 入参：开始时间、结束时间
     * 出参：无
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    /**
     * 规范化调用次数指标
     *
     * 为什么：保证指标字段始终有值，避免空指针
     * 入参：调用次数指标
     * 出参：规范化后的指标
     */
    private CallMetrics normalizeCallMetrics(CallMetrics metrics) {
        if (metrics == null) {
            CallMetrics result = new CallMetrics();
            result.setTotalCalls(0L);
            result.setSuccessCalls(0L);
            result.setFailedCalls(0L);
            result.setAvgResponseTime(0.0);
            result.setTotalTokens(0L);
            return result;
        }
        return metrics;
    }

    /**
     * 规范化成功率指标
     *
     * 为什么：保证成功率口径统一并避免空值
     * 入参：成功率指标
     * 出参：规范化后的指标
     */
    private SuccessRate normalizeSuccessRate(SuccessRate successRate) {
        if (successRate == null) {
            SuccessRate result = new SuccessRate();
            result.setTotalCalls(0L);
            result.setSuccessCalls(0L);
            result.setSuccessRate(0.0);
            return result;
        }
        Long totalCalls = successRate.getTotalCalls();
        Long successCalls = successRate.getSuccessCalls();
        if (totalCalls == null) {
            totalCalls = 0L;
            successRate.setTotalCalls(totalCalls);
        }
        if (successCalls == null) {
            successCalls = 0L;
            successRate.setSuccessCalls(successCalls);
        }
        if (totalCalls > 0) {
            double rate = successCalls * 100.0 / totalCalls;
            successRate.setSuccessRate(rate);
        } else {
            successRate.setSuccessRate(0.0);
        }
        return successRate;
    }

    /**
     * 规范化响应时间指标
     *
     * 为什么：保证时间字段完整并避免空值
     * 入参：响应时间指标
     * 出参：规范化后的指标
     */
    private ResponseTime normalizeResponseTime(ResponseTime responseTime) {
        if (responseTime == null) {
            ResponseTime result = new ResponseTime();
            result.setAvgResponseTime(0.0);
            result.setMinResponseTime(0L);
            result.setMaxResponseTime(0L);
            return result;
        }
        return responseTime;
    }
}
