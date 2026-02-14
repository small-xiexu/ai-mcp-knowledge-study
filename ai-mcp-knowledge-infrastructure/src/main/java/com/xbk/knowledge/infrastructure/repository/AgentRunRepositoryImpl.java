package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.agent.AgentRun;
import com.xbk.knowledge.domain.repository.AgentRunRepository;
import com.xbk.knowledge.infrastructure.mapper.AgentRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * AgentRun 仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class AgentRunRepositoryImpl implements AgentRunRepository {

    private final AgentRunMapper agentRunMapper;

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

    @Override
    public Optional<AgentRun> findByRunId(Long orgId, String runId) {
        if (orgId == null || runId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentRunMapper.findByRunId(orgId, runId));
    }

    @Override
    public int updateStatus(Long orgId, String runId, String status, String errorMessage, LocalDateTime endedAt) {
        if (orgId == null || runId == null || status == null) {
            return 0;
        }
        return agentRunMapper.updateStatus(orgId, runId, status, errorMessage, endedAt);
    }

    @Override
    public int incrementToolCallCount(String runId, Long orgId, int delta) {
        if (orgId == null || runId == null || delta == 0) {
            return 0;
        }
        return agentRunMapper.incrementToolCallCount(orgId, runId, delta);
    }

    @Override
    public int incrementToolDeniedCount(String runId, Long orgId, int delta) {
        if (orgId == null || runId == null || delta == 0) {
            return 0;
        }
        return agentRunMapper.incrementToolDeniedCount(orgId, runId, delta);
    }
}
