package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.vo.CallMetrics;
import com.xbk.knowledge.domain.model.vo.ModelUsage;
import com.xbk.knowledge.domain.model.vo.ResponseTime;
import com.xbk.knowledge.domain.model.vo.SuccessRate;
import com.xbk.knowledge.types.enums.CallStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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
     * @param modelId 模型ID
     * @return 调用日志列表
     */
    List<CallLog> selectByModelId(@Param("modelId") Long modelId);

    /**
     * 根据状态查询调用日志
     *
     * @param status 调用状态
     * @return 调用日志列表
     */
    List<CallLog> selectByStatus(@Param("status") CallStatus status);

    /**
     * 根据时间范围查询调用日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 调用日志列表
     */
    List<CallLog> selectByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定模型的调用次数
     *
     * @param modelId 模型ID
     * @return 调用次数
     */
    long countByModelId(@Param("modelId") Long modelId);

    /**
     * 统计指定模型的调用次数（按状态）
     *
     * @param modelId 模型ID
     * @param status  调用状态
     * @return 调用次数
     */
    long countByModelIdAndStatus(@Param("modelId") Long modelId, @Param("status") CallStatus status);

    /**
     * 聚合统计调用次数
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 调用次数统计
     */
    CallMetrics aggregateCallMetrics(@Param("modelId") Long modelId,
                                     @Param("taskType") String taskType,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合统计成功率
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 成功率统计
     */
    SuccessRate aggregateSuccessRate(@Param("modelId") Long modelId,
                                     @Param("taskType") String taskType,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合统计响应时间
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 响应时间统计
     */
    ResponseTime aggregateResponseTime(@Param("modelId") Long modelId,
                                       @Param("taskType") String taskType,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合统计模型使用分布
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 模型使用分布
     */
    List<ModelUsage> aggregateModelUsage(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
