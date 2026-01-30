package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.vo.CallMetrics;
import com.xbk.knowledge.domain.model.vo.MetricsQuery;
import com.xbk.knowledge.domain.model.vo.ModelUsage;
import com.xbk.knowledge.domain.model.vo.ModelUsageQuery;
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

    /**
     * 统计调用次数指标
     * 统一时间范围校验并规范化返回结构
     */
    @Override
    public CallMetrics collectCallMetrics(MetricsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("指标查询条件不能为空");
        }
        // 验证时间范围
        validateTimeRange(query.getStartTime(), query.getEndTime());

        // 调用仓储聚合数据
        CallMetrics metrics = callLogRepository.aggregateCallMetrics(query);

        // 规范化数据（确保对外返回稳定结构，避免前端空指针）
        return normalizeCallMetrics(metrics);
    }

    /**
     * 统计成功率指标
     * 统一时间范围校验并规范化成功率口径
     */
    @Override
    public SuccessRate collectSuccessRate(MetricsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("指标查询条件不能为空");
        }
        // 验证时间范围
        validateTimeRange(query.getStartTime(), query.getEndTime());

        // 调用仓储聚合数据
        SuccessRate successRate = callLogRepository.aggregateSuccessRate(query);

        // 规范化数据（保证成功率口径统一）
        return normalizeSuccessRate(successRate);
    }

    /**
     * 统计响应时间指标
     * 统一时间范围校验并规范化响应时间数据
     */
    @Override
    public ResponseTime collectResponseTime(MetricsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("指标查询条件不能为空");
        }
        // 验证时间范围
        validateTimeRange(query.getStartTime(), query.getEndTime());

        // 调用仓储聚合数据
        ResponseTime responseTime = callLogRepository.aggregateResponseTime(query);

        // 规范化数据（避免 null 导致图表渲染失败）
        return normalizeResponseTime(responseTime);
    }

    /**
     * 统计模型使用分布
     * 统一时间范围校验并保证返回列表稳定
     */
    @Override
    public List<ModelUsage> collectModelUsage(ModelUsageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("模型使用查询条件不能为空");
        }
        // 验证时间范围
        validateTimeRange(query.getStartTime(), query.getEndTime());

        // 调用仓储聚合数据
        List<ModelUsage> usageList = callLogRepository.aggregateModelUsage(query);

        // 返回结果（如果为 null 则返回空列表）
        return usageList != null ? usageList : Collections.emptyList();
    }

    /**
     * 验证时间范围
     * 防止开始时间晚于结束时间
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    /**
     * 规范化调用次数指标
     * 保证指标字段始终有值，避免空指针
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
     * 保证成功率口径统一并避免空值
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
     * 保证时间字段完整并避免空值
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
