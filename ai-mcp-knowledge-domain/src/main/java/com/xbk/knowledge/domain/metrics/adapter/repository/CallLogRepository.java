package com.xbk.knowledge.domain.metrics.adapter.repository;

import com.xbk.knowledge.domain.metrics.model.aggregate.CallLogAggregate;
import com.xbk.knowledge.domain.metrics.model.entity.CallLog;
import com.xbk.knowledge.domain.metrics.model.valobj.CallMetrics;
import com.xbk.knowledge.domain.metrics.model.valobj.CallStatusQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.MetricsQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelIdQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelIdStatusQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsage;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsageQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ResponseTime;
import com.xbk.knowledge.domain.metrics.model.valobj.SuccessRate;
import com.xbk.knowledge.domain.metrics.model.valobj.TimeRangeQuery;

import java.util.List;

/**
 * 调用日志仓储接口
 * 通过仓储抽象隔离持久化实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author sxie
 */
public interface CallLogRepository {

    /**
     * 保存调用日志聚合
     *
     * 以聚合形式保存调用日志，保证一致性
     * 
     * @param aggregate 调用日志聚合根。
     * @return 持久化后的调用日志聚合根。
     */
    CallLogAggregate save(CallLogAggregate aggregate);

    /**
     * 根据模型ID查询调用日志
     *
     * 按模型维度查看调用记录
     * 
     * @param query 主键查询条件。
     * @return 调用日志列表。
     */
    List<CallLog> findByModelId(ModelIdQuery query);

    /**
     * 根据状态查询调用日志
     *
     * 按状态查看成功/失败记录
     * 
     * @param query 调用状态查询条件。
     * @return 调用日志列表。
     */
    List<CallLog> findByStatus(CallStatusQuery query);

    /**
     * 根据时间范围查询调用日志
     *
     * 按时间范围过滤调用记录
     * 
     * @param query 时间范围查询条件。
     * @return 调用日志列表。
     */
    List<CallLog> findByCreatedAtBetween(TimeRangeQuery query);

    /**
     * 统计指定模型的调用次数
     *
     * 按模型统计调用量
     * 
     * @param query 主键查询条件。
     * @return 统计数量。
     */
    long countByModelId(ModelIdQuery query);

    /**
     * 统计指定模型的调用次数（按状态）
     *
     * 按状态统计模型调用量
     * 
     * @param query 模型+状态联合查询条件。
     * @return 统计数量。
     */
    long countByModelIdAndStatus(ModelIdStatusQuery query);

    /**
     * 聚合统计调用次数
     *
     * 支持监控大盘统计
     * 
     * @param query 时间范围查询条件。
     * @return 调用次数聚合指标。
     */
    CallMetrics aggregateCallMetrics(MetricsQuery query);

    /**
     * 聚合统计成功率
     *
     * 支持监控大盘统计
     * 
     * @param query 时间范围查询条件。
     * @return 成功率聚合指标。
     */
    SuccessRate aggregateSuccessRate(MetricsQuery query);

    /**
     * 聚合统计响应时间
     *
     * 支持监控大盘统计
     * 
     * @param query 时间范围查询条件。
     * @return 响应时间聚合指标。
     */
    ResponseTime aggregateResponseTime(MetricsQuery query);

    /**
     * 聚合统计模型使用分布
     *
     * 支持监控大盘统计
     * 
     * @param query 模型使用分布查询条件。
     * @return 监控指标列表。
     */
    List<ModelUsage> aggregateModelUsage(ModelUsageQuery query);
}
