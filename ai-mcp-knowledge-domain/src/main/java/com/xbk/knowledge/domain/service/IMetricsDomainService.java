package com.xbk.knowledge.domain.service;

import com.xbk.knowledge.domain.model.vo.metrics.CallMetrics;
import com.xbk.knowledge.domain.model.vo.metrics.MetricsQuery;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsage;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsageQuery;
import com.xbk.knowledge.domain.model.vo.metrics.ResponseTime;
import com.xbk.knowledge.domain.model.vo.metrics.SuccessRate;

import java.util.List;

/**
 * 监控指标领域服务接口
 * 负责监控指标的业务逻辑处理
 *
 * 职责：领域服务接口，用于定义业务能力
 * @author xiexu
 */
public interface IMetricsDomainService {

    /**
     * 统计调用次数指标
     *
     * @param query 指标查询条件
     * @return 调用次数统计
     */
    CallMetrics collectCallMetrics(MetricsQuery query);

    /**
     * 统计成功率指标
     *
     * @param query 指标查询条件
     * @return 成功率统计
     */
    SuccessRate collectSuccessRate(MetricsQuery query);

    /**
     * 统计响应时间指标
     *
     * @param query 指标查询条件
     * @return 响应时间统计
     */
    ResponseTime collectResponseTime(MetricsQuery query);

    /**
     * 统计模型使用分布
     *
     * @param query 模型使用查询条件
     * @return 模型使用分布列表
     */
    List<ModelUsage> collectModelUsage(ModelUsageQuery query);
}
