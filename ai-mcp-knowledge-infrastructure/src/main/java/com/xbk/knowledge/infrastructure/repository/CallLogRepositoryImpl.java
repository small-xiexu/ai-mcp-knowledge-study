package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.vo.CallMetrics;
import com.xbk.knowledge.domain.model.vo.ModelUsage;
import com.xbk.knowledge.domain.model.vo.ResponseTime;
import com.xbk.knowledge.domain.model.vo.SuccessRate;
import com.xbk.knowledge.domain.repository.CallLogRepository;
import com.xbk.knowledge.infrastructure.mapper.CallLogMapper;
import com.xbk.knowledge.types.enums.CallStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 调用日志仓储实现
 * 通过 Mapper 执行 XML SQL，隔离持久化细节
 *
 * 职责：仓储实现，用于落地数据访问
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class CallLogRepositoryImpl implements CallLogRepository {

    private final CallLogMapper callLogMapper;

    @Override
    public CallLog save(CallLog callLog) {
        if (callLog.getCreatedAt() == null) {
            callLog.setCreatedAt(LocalDateTime.now());
        }
        callLogMapper.insertCallLog(callLog);
        return callLog;
    }

    @Override
    public List<CallLog> findByModelId(Long modelId) {
        return callLogMapper.selectByModelId(modelId);
    }

    @Override
    public List<CallLog> findByStatus(CallStatus status) {
        return callLogMapper.selectByStatus(status);
    }

    @Override
    public List<CallLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return callLogMapper.selectByCreatedAtBetween(startTime, endTime);
    }

    @Override
    public long countByModelId(Long modelId) {
        return callLogMapper.countByModelId(modelId);
    }

    @Override
    public long countByModelIdAndStatus(Long modelId, CallStatus status) {
        return callLogMapper.countByModelIdAndStatus(modelId, status);
    }

    @Override
    public CallMetrics aggregateCallMetrics(Long modelId, String taskType, LocalDateTime startTime, LocalDateTime endTime) {
        return callLogMapper.aggregateCallMetrics(modelId, taskType, startTime, endTime);
    }

    @Override
    public SuccessRate aggregateSuccessRate(Long modelId, String taskType, LocalDateTime startTime, LocalDateTime endTime) {
        return callLogMapper.aggregateSuccessRate(modelId, taskType, startTime, endTime);
    }

    @Override
    public ResponseTime aggregateResponseTime(Long modelId, String taskType, LocalDateTime startTime, LocalDateTime endTime) {
        return callLogMapper.aggregateResponseTime(modelId, taskType, startTime, endTime);
    }

    @Override
    public List<ModelUsage> aggregateModelUsage(LocalDateTime startTime, LocalDateTime endTime) {
        return callLogMapper.aggregateModelUsage(startTime, endTime);
    }
}
