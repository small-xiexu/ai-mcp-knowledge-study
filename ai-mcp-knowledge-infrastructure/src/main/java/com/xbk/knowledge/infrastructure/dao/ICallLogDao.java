package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.CallLogPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 调用日志 Mapper
 * 统一通过 XML 承载 SQL，避免注解式查询
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 * @author sxie
 */
@Mapper
public interface ICallLogDao extends BaseMapper<CallLogPO> {

    /**
     * 新增调用日志
     *
     * 落库记录调用信息
     * 
     * @param callLog 调用日志持久化实体。
     * @return 影响行数。
     */
    int insertCallLog(CallLogPO callLog);

    /**
     * 根据模型ID查询调用日志
     *
     * 按模型维度查询日志
     * 
     * @param query 主键查询条件。
     * @return CallLogPO 列表。
     */
    List<CallLogPO> selectByModelId(ModelIdQuery query);

    /**
     * 根据状态查询调用日志
     *
     * 按状态筛选日志
     * 
     * @param query 调用状态查询条件。
     * @return CallLogPO 列表。
     */
    List<CallLogPO> selectByStatus(CallStatusQuery query);

    /**
     * 根据时间范围查询调用日志
     *
     * 按时间范围筛选日志
     * 
     * @param query 时间范围查询条件。
     * @return CallLogPO 列表。
     */
    List<CallLogPO> selectByCreatedAtBetween(TimeRangeQuery query);

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
     * 按状态统计调用量
     * 
     * @param query 模型与状态查询条件。
     * @return 统计数量。
     */
    long countByModelIdAndStatus(ModelIdStatusQuery query);

    /**
     * 聚合统计调用次数
     *
     * 为监控统计提供数据
     * 
     * @param query 时间范围查询条件。
     * @return 调用次数指标。
     */
    CallMetrics aggregateCallMetrics(MetricsQuery query);

    /**
     * 聚合统计成功率
     *
     * 为监控统计提供数据
     * 
     * @param query 时间范围查询条件。
     * @return 成功率指标。
     */
    SuccessRate aggregateSuccessRate(MetricsQuery query);

    /**
     * 聚合统计响应时间
     *
     * 为监控统计提供数据
     * 
     * @param query 时间范围查询条件。
     * @return 响应时间指标。
     */
    ResponseTime aggregateResponseTime(MetricsQuery query);

    /**
     * 聚合统计模型使用分布
     *
     * 为监控统计提供数据
     * 
     * @param query 模型使用分布查询条件。
     * @return 监控指标列表。
     */
    List<ModelUsage> aggregateModelUsage(ModelUsageQuery query);
}
