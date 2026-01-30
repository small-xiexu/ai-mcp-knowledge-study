package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.aggregate.call.CallLogAggregate;
import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.vo.metrics.CallMetrics;
import com.xbk.knowledge.domain.model.vo.metrics.CallStatusQuery;
import com.xbk.knowledge.domain.model.vo.metrics.MetricsQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelIdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelIdStatusQuery;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsage;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsageQuery;
import com.xbk.knowledge.domain.model.vo.metrics.ResponseTime;
import com.xbk.knowledge.domain.model.vo.metrics.SuccessRate;
import com.xbk.knowledge.domain.model.vo.metrics.TimeRangeQuery;

import java.util.List;

/**
 * 调用日志仓储接口
 * 通过仓储抽象隔离持久化实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author xiexu
 */
public interface CallLogRepository {

    /**
     * 保存调用日志聚合
     *
     * @param aggregate 调用日志聚合
     * @return 保存后的聚合
     */
    CallLogAggregate save(CallLogAggregate aggregate);

    /**
     * 根据模型ID查询调用日志
     *
     * @param query 模型ID查询条件
     * @return 调用日志列表
     */
    List<CallLog> findByModelId(ModelIdQuery query);

    /**
     * 根据状态查询调用日志
     *
     * @param query 调用状态查询条件
     * @return 调用日志列表
     */
    List<CallLog> findByStatus(CallStatusQuery query);

    /**
     * 根据时间范围查询调用日志
     *
     * @param query 时间范围查询条件
     * @return 调用日志列表
     */
    List<CallLog> findByCreatedAtBetween(TimeRangeQuery query);

    /**
     * 统计指定模型的调用次数
     *
     * @param query 模型ID查询条件
     * @return 调用次数
     */
    long countByModelId(ModelIdQuery query);

    /**
     * 统计指定模型的调用次数（按状态）
     *
     * @param query 模型ID与调用状态查询条件
     * @return 调用次数
     */
    long countByModelIdAndStatus(ModelIdStatusQuery query);

    /**
     * 聚合统计调用次数
     *
     * @param query 指标查询条件
     * @return 调用次数统计
     */
    CallMetrics aggregateCallMetrics(MetricsQuery query);

    /**
     * 聚合统计成功率
     *
     * @param query 指标查询条件
     * @return 成功率统计
     */
    SuccessRate aggregateSuccessRate(MetricsQuery query);

    /**
     * 聚合统计响应时间
     *
     * @param query 指标查询条件
     * @return 响应时间统计
     */
    ResponseTime aggregateResponseTime(MetricsQuery query);

    /**
     * 聚合统计模型使用分布
     *
     * @param query 模型使用查询条件
     * @return 模型使用分布
     */
    List<ModelUsage> aggregateModelUsage(ModelUsageQuery query);
}
