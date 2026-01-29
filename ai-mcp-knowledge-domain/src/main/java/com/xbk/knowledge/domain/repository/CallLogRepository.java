package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.vo.CallMetrics;
import com.xbk.knowledge.domain.model.vo.ModelUsage;
import com.xbk.knowledge.domain.model.vo.ResponseTime;
import com.xbk.knowledge.domain.model.vo.SuccessRate;
import com.xbk.knowledge.types.enums.CallStatus;

import java.time.LocalDateTime;
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
     * 保存调用日志
     *
     * @param callLog 调用日志
     * @return 保存后的日志
     */
    CallLog save(CallLog callLog);

    /**
     * 根据模型ID查询调用日志
     *
     * @param modelId 模型ID
     * @return 调用日志列表
     */
    List<CallLog> findByModelId(Long modelId);

    /**
     * 根据状态查询调用日志
     *
     * @param status 调用状态
     * @return 调用日志列表
     */
    List<CallLog> findByStatus(CallStatus status);

    /**
     * 根据时间范围查询调用日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 调用日志列表
     */
    List<CallLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定模型的调用次数
     *
     * @param modelId 模型ID
     * @return 调用次数
     */
    long countByModelId(Long modelId);

    /**
     * 统计指定模型的调用次数（按状态）
     *
     * @param modelId 模型ID
     * @param status  调用状态
     * @return 调用次数
     */
    long countByModelIdAndStatus(Long modelId, CallStatus status);

    /**
     * 聚合统计调用次数
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 调用次数统计
     */
    CallMetrics aggregateCallMetrics(Long modelId, String taskType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 聚合统计成功率
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 成功率统计
     */
    SuccessRate aggregateSuccessRate(Long modelId, String taskType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 聚合统计响应时间
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 响应时间统计
     */
    ResponseTime aggregateResponseTime(Long modelId, String taskType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 聚合统计模型使用分布
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 模型使用分布
     */
    List<ModelUsage> aggregateModelUsage(LocalDateTime startTime, LocalDateTime endTime);
}
