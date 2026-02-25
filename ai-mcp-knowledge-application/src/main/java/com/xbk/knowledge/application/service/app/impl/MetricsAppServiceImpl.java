package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.MetricsAppService;
import com.xbk.knowledge.domain.metrics.model.valobj.CallMetrics;
import com.xbk.knowledge.domain.metrics.model.valobj.MetricsQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsage;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsageQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ResponseTime;
import com.xbk.knowledge.domain.metrics.model.valobj.SuccessRate;
import com.xbk.knowledge.domain.metrics.service.IMetricsDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监控指标应用服务实现
 * 负责指标查询用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class MetricsAppServiceImpl implements MetricsAppService {
    /**
     * 指标领域服务，用于聚合调用量、成功率、耗时与模型分布。
     */
    private final IMetricsDomainService metricsDomainService;

    /**
     * 统计调用次数指标
     *
     * 统一调用统计入口，便于后续扩展维度
     * 
     * @param query 时间范围查询条件。
     * @return 调用次数聚合指标。
     */
    @Override
    public CallMetrics collectCallMetrics(MetricsQuery query) {
        return metricsDomainService.collectCallMetrics(query);
    }

    /**
     * 统计成功率指标
     *
     * 统一成功率统计入口，避免前端自行计算
     * 
     * @param query 时间范围查询条件。
     * @return 成功率聚合指标。
     */
    @Override
    public SuccessRate collectSuccessRate(MetricsQuery query) {
        return metricsDomainService.collectSuccessRate(query);
    }

    /**
     * 统计响应时间指标
     *
     * 统一响应时间统计入口，便于趋势分析
     * 
     * @param query 时间范围查询条件。
     * @return 响应时间聚合指标。
     */
    @Override
    public ResponseTime collectResponseTime(MetricsQuery query) {
        return metricsDomainService.collectResponseTime(query);
    }

    /**
     * 统计模型使用分布
     *
     * 统一模型使用统计入口，便于资源规划
     * 
     * @param query 模型使用分布查询条件。
     * @return 监控指标列表。
     */
    @Override
    public List<ModelUsage> collectModelUsage(ModelUsageQuery query) {
        return metricsDomainService.collectModelUsage(query);
    }
}
