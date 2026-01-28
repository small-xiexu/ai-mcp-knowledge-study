package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.dto.DomainCallMetricsDTO;
import com.xbk.knowledge.domain.model.dto.DomainModelUsageDTO;
import com.xbk.knowledge.domain.model.dto.DomainResponseTimeDTO;
import com.xbk.knowledge.domain.model.dto.DomainSuccessRateDTO;
import com.xbk.knowledge.types.enums.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 调用日志 Repository
 *
 * @author xiexu
 */
@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {

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
     * 统计指定模型的成功调用次数
     *
     * @param modelId 模型ID
     * @param status  调用状态
     * @return 成功调用次数
     */
    long countByModelIdAndStatus(Long modelId, CallStatus status);

    /**
     * 聚合统计调用次数
     * 统一在数据库层完成统计，减少接口层计算成本
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 调用次数统计
     */
    @Query("""
            select new com.xbk.knowledge.orchestration.model.dto.CallMetricsDTO(
                count(c),
                coalesce(sum(case when c.status = com.xbk.knowledge.orchestration.model.enums.CallStatus.SUCCESS then 1 else 0 end), 0),
                coalesce(sum(case when c.status = com.xbk.knowledge.orchestration.model.enums.CallStatus.FAILED then 1 else 0 end), 0),
                coalesce(sum(case when c.status = com.xbk.knowledge.orchestration.model.enums.CallStatus.FALLBACK then 1 else 0 end), 0)
            )
            from CallLog c
            where (:modelId is null or c.modelId = :modelId)
              and (:taskType is null or c.taskType = :taskType)
              and (:startTime is null or c.createdAt >= :startTime)
              and (:endTime is null or c.createdAt <= :endTime)
            """)
    DomainCallMetricsDTO aggregateCallMetrics(@Param("modelId") Long modelId,
                                        @Param("taskType") String taskType,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合统计成功率
     * 仅返回必要聚合字段，成功率由服务层统一校准
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 成功率统计
     */
    @Query("""
            select new com.xbk.knowledge.orchestration.model.dto.SuccessRateDTO(
                count(c),
                coalesce(sum(case when c.status = com.xbk.knowledge.orchestration.model.enums.CallStatus.SUCCESS then 1 else 0 end), 0),
                0.0
            )
            from CallLog c
            where (:modelId is null or c.modelId = :modelId)
              and (:taskType is null or c.taskType = :taskType)
              and (:startTime is null or c.createdAt >= :startTime)
              and (:endTime is null or c.createdAt <= :endTime)
            """)
    DomainSuccessRateDTO aggregateSuccessRate(@Param("modelId") Long modelId,
                                        @Param("taskType") String taskType,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合统计响应时间
     * 统一在数据库层完成平均值和极值统计
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 响应时间统计
     */
    @Query("""
            select new com.xbk.knowledge.orchestration.model.dto.ResponseTimeDTO(
                coalesce(avg(c.responseTime), 0),
                coalesce(max(c.responseTime), 0),
                coalesce(min(c.responseTime), 0)
            )
            from CallLog c
            where (:modelId is null or c.modelId = :modelId)
              and (:taskType is null or c.taskType = :taskType)
              and (:startTime is null or c.createdAt >= :startTime)
              and (:endTime is null or c.createdAt <= :endTime)
            """)
    DomainResponseTimeDTO aggregateResponseTime(@Param("modelId") Long modelId,
                                          @Param("taskType") String taskType,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合统计模型使用分布
     * 仅返回模型维度的调用次数，使用率由服务层统一计算
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 模型使用分布
     */
    @Query("""
            select new com.xbk.knowledge.orchestration.model.dto.ModelUsageDTO(
                c.modelId,
                count(c),
                0.0
            )
            from CallLog c
            where (:startTime is null or c.createdAt >= :startTime)
              and (:endTime is null or c.createdAt <= :endTime)
            group by c.modelId
            """)
    List<DomainModelUsageDTO> aggregateModelUsage(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
}
