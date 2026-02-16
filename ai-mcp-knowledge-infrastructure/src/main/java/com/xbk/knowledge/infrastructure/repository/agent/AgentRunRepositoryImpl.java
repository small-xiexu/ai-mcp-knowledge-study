package com.xbk.knowledge.infrastructure.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.AgentRun;
import com.xbk.knowledge.domain.repository.agent.AgentRunRepository;
import com.xbk.knowledge.infrastructure.mapper.agent.AgentRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * AgentRun 仓储实现。
 
  * @author xiexu
  */
@Repository
@RequiredArgsConstructor
public class AgentRunRepositoryImpl implements AgentRunRepository {

    private final AgentRunMapper agentRunMapper;

    /**
     * insert。
     *
     * @param run 参数
     * @return 返回结果
     */
    @Override
    public AgentRun insert(AgentRun run) {
        if (run == null) {
            return null;
        }
        if (run.getStartedAt() == null) {
            run.setStartedAt(LocalDateTime.now());
        }
        agentRunMapper.insertRun(run);
        return run;
    }

    /**
     * updateStatusAndMetrics。
     *
     * @param run 参数
     * @return 返回结果
     */
    @Override
    public int updateStatusAndMetrics(AgentRun run) {
        if (run == null || run.getOrgId() == null || run.getRunId() == null) {
            return 0;
        }
        if (run.getEndedAt() == null) {
            run.setEndedAt(LocalDateTime.now());
        }
        return agentRunMapper.updateStatusAndMetrics(run);
    }

    /**
     * findByRunId。
     *
     * @param orgId 参数
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentRun> findByRunId(Long orgId, String runId) {
        if (orgId == null || runId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentRunMapper.findByRunId(orgId, runId));
    }

    /**
     * updateStatus。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param status 参数
     * @param errorMessage 参数
     * @param endedAt 参数
     * @return 返回结果
     */
    @Override
    public int updateStatus(Long orgId, String runId, String status, String errorMessage, LocalDateTime endedAt) {
        if (orgId == null || runId == null || status == null) {
            return 0;
        }
        return agentRunMapper.updateStatus(orgId, runId, status, errorMessage, endedAt);
    }

    /**
     * incrementToolCallCount。
     *
     * @param runId 参数
     * @param orgId 参数
     * @param delta 参数
     * @return 返回结果
     */
    @Override
    public int incrementToolCallCount(String runId, Long orgId, int delta) {
        if (orgId == null || runId == null || delta == 0) {
            return 0;
        }
        return agentRunMapper.incrementToolCallCount(orgId, runId, delta);
    }

    /**
     * incrementToolDeniedCount。
     *
     * @param runId 参数
     * @param orgId 参数
     * @param delta 参数
     * @return 返回结果
     */
    @Override
    public int incrementToolDeniedCount(String runId, Long orgId, int delta) {
        if (orgId == null || runId == null || delta == 0) {
            return 0;
        }
        return agentRunMapper.incrementToolDeniedCount(orgId, runId, delta);
    }
}
