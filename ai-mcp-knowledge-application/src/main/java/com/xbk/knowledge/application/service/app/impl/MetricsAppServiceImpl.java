package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.MetricsAppService;
import com.xbk.knowledge.domain.model.vo.metrics.CallMetrics;
import com.xbk.knowledge.domain.model.vo.metrics.MetricsQuery;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsage;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsageQuery;
import com.xbk.knowledge.domain.model.vo.metrics.ResponseTime;
import com.xbk.knowledge.domain.model.vo.metrics.SuccessRate;
import com.xbk.knowledge.domain.service.metrics.IMetricsDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监控指标应用服务实现
 * 负责指标查询用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class MetricsAppServiceImpl implements MetricsAppService {

    private final IMetricsDomainService metricsDomainService;

    /**
     * 统计调用次数指标
     *
     * 为什么：统一调用统计入口，便于后续扩展维度
     * 入参：指标查询对象
     * 出参：调用次数统计
     */
    @Override
    public CallMetrics collectCallMetrics(MetricsQuery query) {
        return metricsDomainService.collectCallMetrics(query);
    }

    /**
     * 统计成功率指标
     *
     * 为什么：统一成功率统计入口，避免前端自行计算
     * 入参：指标查询对象
     * 出参：成功率统计
     */
    @Override
    public SuccessRate collectSuccessRate(MetricsQuery query) {
        return metricsDomainService.collectSuccessRate(query);
    }

    /**
     * 统计响应时间指标
     *
     * 为什么：统一响应时间统计入口，便于趋势分析
     * 入参：指标查询对象
     * 出参：响应时间统计
     */
    @Override
    public ResponseTime collectResponseTime(MetricsQuery query) {
        return metricsDomainService.collectResponseTime(query);
    }

    /**
     * 统计模型使用分布
     *
     * 为什么：统一模型使用统计入口，便于资源规划
     * 入参：模型使用查询对象
     * 出参：模型使用分布
     */
    @Override
    public List<ModelUsage> collectModelUsage(ModelUsageQuery query) {
        return metricsDomainService.collectModelUsage(query);
    }
}
