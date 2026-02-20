package com.xbk.knowledge.infrastructure.repository.metrics;

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
import com.xbk.knowledge.domain.metrics.adapter.repository.CallLogRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.ICallLogDao;
import com.xbk.knowledge.infrastructure.dao.po.CallLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 调用日志仓储实现
 * 通过 Mapper 执行 XML SQL，隔离持久化细节
 *
 * 职责：仓储实现，用于落地数据访问
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class CallLogRepositoryImpl implements CallLogRepository {

    private final ICallLogDao callLogMapper;

    /**
     * 保存调用日志
     * 统一补齐创建时间，保证日志可追溯
     *
     * 为什么：保证日志具备时间戳便于审计
     * 入参：调用日志聚合
     * 出参：保存后的聚合
     */
    @Override
    public CallLogAggregate save(CallLogAggregate aggregate) {
        if (aggregate == null || aggregate.getCallLog() == null) {
            return aggregate;
        }
        CallLog callLog = aggregate.getCallLog();
        if (callLog.getCreatedAt() == null) {
            LocalDateTime createdAt = LocalDateTime.now();
            callLog.setCreatedAt(createdAt);
        }
        /*
         * 目的：统一落库入口，避免重复插入逻辑
 */
        callLogMapper.insertCallLog(BeanMappingUtils.map(callLog, CallLogPO.class));
        aggregate.setCallLog(callLog);
        return aggregate;
    }

    /**
     * 按模型 ID 查询调用日志
     * 用于模型维度的日志回溯
     *
     * 为什么：模型维度追踪调用记录
     * 入参：模型ID查询条件
     * 出参：调用日志列表
     */
    @Override
    public List<CallLog> findByModelId(ModelIdQuery query) {
        if (query == null || query.getModelId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(callLogMapper.selectByModelId(query), CallLog.class);
    }

    /**
     * 按调用状态查询日志
     * 用于失败/成功场景快速筛查
     *
     * 为什么：快速筛选成功/失败记录
     * 入参：调用状态查询条件
     * 出参：调用日志列表
     */
    @Override
    public List<CallLog> findByStatus(CallStatusQuery query) {
        if (query == null || query.getStatus() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(callLogMapper.selectByStatus(query), CallLog.class);
    }

    /**
     * 按时间区间查询日志
     * 用于时间窗口统计与排查
     *
     * 为什么：按时间范围筛选日志
     * 入参：时间范围查询条件
     * 出参：调用日志列表
     */
    @Override
    public List<CallLog> findByCreatedAtBetween(TimeRangeQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(callLogMapper.selectByCreatedAtBetween(query), CallLog.class);
    }

    /**
     * 统计某模型调用次数
     * 用于模型维度统计
     *
     * 为什么：按模型统计调用量
     * 入参：模型ID查询条件
     * 出参：调用次数
     */
    @Override
    public long countByModelId(ModelIdQuery query) {
        if (query == null || query.getModelId() == null) {
            return 0L;
        }
        return callLogMapper.countByModelId(query);
    }

    /**
     * 统计某模型指定状态调用次数
     * 用于成功/失败比例分析
     *
     * 为什么：按状态统计调用量
     * 入参：模型ID与状态查询条件
     * 出参：调用次数
     */
    @Override
    public long countByModelIdAndStatus(ModelIdStatusQuery query) {
        if (query == null || query.getModelId() == null || query.getStatus() == null) {
            return 0L;
        }
        return callLogMapper.countByModelIdAndStatus(query);
    }

    /**
     * 聚合调用次数指标
     * 交由数据库统计以降低应用层计算成本
     *
     * 为什么：数据库层聚合更高效
     * 入参：指标查询条件
     * 出参：调用次数指标
     */
    @Override
    public CallMetrics aggregateCallMetrics(MetricsQuery query) {
        return callLogMapper.aggregateCallMetrics(query);
    }

    /**
     * 聚合成功率指标
     * 由数据库聚合确保口径一致
     *
     * 为什么：数据库聚合可保证口径统一
     * 入参：指标查询条件
     * 出参：成功率指标
     */
    @Override
    public SuccessRate aggregateSuccessRate(MetricsQuery query) {
        return callLogMapper.aggregateSuccessRate(query);
    }

    /**
     * 聚合响应时间指标
     * 统一在数据库层计算平均/最小/最大值
     *
     * 为什么：数据库层聚合可降低应用层成本
     * 入参：指标查询条件
     * 出参：响应时间指标
     */
    @Override
    public ResponseTime aggregateResponseTime(MetricsQuery query) {
        return callLogMapper.aggregateResponseTime(query);
    }

    /**
     * 聚合模型使用分布
     * 用于统计不同模型调用占比
     *
     * 为什么：数据库聚合便于按模型统计
     * 入参：模型使用查询条件
     * 出参：模型使用分布
     */
    @Override
    public List<ModelUsage> aggregateModelUsage(ModelUsageQuery query) {
        return callLogMapper.aggregateModelUsage(query);
    }
}
