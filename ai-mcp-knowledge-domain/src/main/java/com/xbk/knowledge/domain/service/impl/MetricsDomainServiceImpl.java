package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.vo.CallMetrics;
import com.xbk.knowledge.domain.model.vo.ModelUsage;
import com.xbk.knowledge.domain.model.vo.ResponseTime;
import com.xbk.knowledge.domain.model.vo.SuccessRate;
import com.xbk.knowledge.domain.repository.CallLogRepository;
import com.xbk.knowledge.domain.service.IMetricsDomainService;
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
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsDomainServiceImpl implements IMetricsDomainService {

    private final CallLogRepository callLogRepository;

    @Override
    public CallMetrics collectCallMetrics(Long modelId, String taskType,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        // 验证时间范围
        validateTimeRange(startTime, endTime);

        // 调用仓储聚合数据
        CallMetrics metrics = callLogRepository.aggregateCallMetrics(modelId, taskType, startTime, endTime);

        // 规范化数据（确保对外返回稳定结构，避免前端空指针）
        return normalizeCallMetrics(metrics);
    }

    @Override
    public SuccessRate collectSuccessRate(Long modelId, String taskType,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        // 验证时间范围
        validateTimeRange(startTime, endTime);

        // 调用仓储聚合数据
        SuccessRate successRate = callLogRepository.aggregateSuccessRate(modelId, taskType, startTime, endTime);

        // 规范化数据（保证成功率口径统一）
        return normalizeSuccessRate(successRate);
    }

    @Override
    public ResponseTime collectResponseTime(Long modelId, String taskType,
                                             LocalDateTime startTime, LocalDateTime endTime) {
        // 验证时间范围
        validateTimeRange(startTime, endTime);

        // 调用仓储聚合数据
        ResponseTime responseTime = callLogRepository.aggregateResponseTime(modelId, taskType, startTime, endTime);

        // 规范化数据（避免 null 导致图表渲染失败）
        return normalizeResponseTime(responseTime);
    }

    @Override
    public List<ModelUsage> collectModelUsage(LocalDateTime startTime, LocalDateTime endTime) {
        // 验证时间范围
        validateTimeRange(startTime, endTime);

        // 调用仓储聚合数据
        List<ModelUsage> usageList = callLogRepository.aggregateModelUsage(startTime, endTime);

        // 返回结果（如果为 null 则返回空列表）
        return usageList != null ? usageList : Collections.emptyList();
    }

    /**
     * 验证时间范围
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    /**
     * 规范化调用次数指标
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
     */
    private SuccessRate normalizeSuccessRate(SuccessRate successRate) {
        if (successRate == null) {
            SuccessRate result = new SuccessRate();
            result.setTotalCalls(0L);
            result.setSuccessCalls(0L);
            result.setSuccessRate(0.0);
            return result;
        }
        return successRate;
    }

    /**
     * 规范化响应时间指标
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
