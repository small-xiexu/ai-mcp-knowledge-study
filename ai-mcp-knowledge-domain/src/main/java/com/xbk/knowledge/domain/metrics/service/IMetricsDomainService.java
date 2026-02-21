package com.xbk.knowledge.domain.metrics.service;

import com.xbk.knowledge.domain.metrics.model.valobj.CallMetrics;
import com.xbk.knowledge.domain.metrics.model.valobj.MetricsQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsage;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsageQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ResponseTime;
import com.xbk.knowledge.domain.metrics.model.valobj.SuccessRate;

import java.util.List;

/**
 * 监控指标领域服务接口
 * 负责监控指标的业务逻辑处理
 *
 * 职责：领域服务接口，用于定义业务能力
 * @author sxie
 */
public interface IMetricsDomainService {

    /**
     * 统计调用次数指标
     *
     * 为什么：为监控面板提供基础统计数据
     * 入参：指标查询条件
     * 出参：调用次数统计
     */
    CallMetrics collectCallMetrics(MetricsQuery query);

    /**
     * 统计成功率指标
     *
     * 为什么：为监控面板提供成功率统计
     * 入参：指标查询条件
     * 出参：成功率统计
     */
    SuccessRate collectSuccessRate(MetricsQuery query);

    /**
     * 统计响应时间指标
     *
     * 为什么：为监控面板提供响应时间统计
     * 入参：指标查询条件
     * 出参：响应时间统计
     */
    ResponseTime collectResponseTime(MetricsQuery query);

    /**
     * 统计模型使用分布
     *
     * 为什么：为监控面板提供模型使用分布
     * 入参：模型使用查询条件
     * 出参：模型使用分布列表
     */
    List<ModelUsage> collectModelUsage(ModelUsageQuery query);
}
