package com.xbk.knowledge.orchestration.domain.repository;

import com.xbk.knowledge.orchestration.domain.entity.CallLog;
import com.xbk.knowledge.orchestration.model.enums.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
