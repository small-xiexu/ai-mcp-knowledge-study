package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 调用日志 Mapper
 * 统一通过 XML 承载 SQL，避免注解式查询
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 * @author xiexu
 */
@Mapper
public interface CallLogMapper extends BaseMapper<CallLog> {

    /**
     * 新增调用日志
     *
     * @param callLog 调用日志
     * @return 影响行数
     */
    int insertCallLog(CallLog callLog);

    /**
     * 根据模型ID查询调用日志
     *
     * @param query 模型ID查询条件
     * @return 调用日志列表
     */
    List<CallLog> selectByModelId(ModelIdQuery query);

    /**
     * 根据状态查询调用日志
     *
     * @param query 调用状态查询条件
     * @return 调用日志列表
     */
    List<CallLog> selectByStatus(CallStatusQuery query);

    /**
     * 根据时间范围查询调用日志
     *
     * @param query 时间范围查询条件
     * @return 调用日志列表
     */
    List<CallLog> selectByCreatedAtBetween(TimeRangeQuery query);

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
