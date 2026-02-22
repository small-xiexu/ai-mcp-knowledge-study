package com.xbk.knowledge.infrastructure.agent.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentRun;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunRepository;
import com.xbk.knowledge.infrastructure.dao.IAgentRunDao;
import com.xbk.knowledge.infrastructure.dao.po.AgentRunPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * AgentRun 仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class AgentRunRepositoryImpl implements AgentRunRepository {

    private final IAgentRunDao agentRunDao;

    /**
     * insert。
     *
     * @param run 参数
     */
    @Override
    public void insert(AgentRun run) {
        if (run == null) {
            return;
        }
        if (run.getStartedAt() == null) {
            run.setStartedAt(LocalDateTime.now());
        }
        agentRunDao.insertRun(toPO(run));
    }

    /**
     * updateStatusAndMetrics。
     *
     * @param run 参数
     */
    @Override
    public void updateStatusAndMetrics(AgentRun run) {
        if (run == null || run.getRunId() == null) {
            return;
        }
        if (run.getEndedAt() == null) {
            run.setEndedAt(LocalDateTime.now());
        }
        agentRunDao.updateStatusAndMetrics(toPO(run));
    }

    /**
     * findByRunId。
     *
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentRun> findByRunId(String runId) {
        if (runId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toEntity(agentRunDao.findByRunId(runId)));
    }

    /**
     * updateStatus。
     *
     * @param runId 参数
     * @param status 参数
     * @param errorMessage 参数
     * @param endedAt 参数
     * @return 返回结果
     */
    @Override
    public int updateStatus(String runId, String status, String errorMessage, LocalDateTime endedAt) {
        if (runId == null || status == null) {
            return 0;
        }
        return agentRunDao.updateStatus(runId, status, errorMessage, endedAt);
    }

    /**
     * incrementToolCallCount。
     *
     * @param runId 参数
     * @param delta 参数
     * @return 返回结果
     */
    @Override
    public int incrementToolCallCount(String runId, int delta) {
        if (runId == null || delta == 0) {
            return 0;
        }
        return agentRunDao.incrementToolCallCount(runId, delta);
    }

    /**
     * incrementToolDeniedCount。
     *
     * @param runId 参数
     * @param delta 参数
     * @return 返回结果
     */
    @Override
    public int incrementToolDeniedCount(String runId, int delta) {
        if (runId == null || delta == 0) {
            return 0;
        }
        return agentRunDao.incrementToolDeniedCount(runId, delta);
    }

    /**
     * deleteByAgentId。
     *
     * @param agentId 参数
     * @return 返回结果
     */
    @Override
    public int deleteByAgentId(Long agentId) {
        if (agentId == null) {
            return 0;
        }
        return agentRunDao.deleteByAgentId(agentId);
    }

    /**
     * 实体转持久化对象。
     */
    private AgentRunPO toPO(AgentRun run) {
        if (run == null) {
            return null;
        }
        return AgentRunPO.builder()
                .runId(run.getRunId())
                .agentId(run.getAgentId())
                .agentCode(run.getAgentCode())
                .agentVersionId(run.getAgentVersionId())
                .runType(run.getRunType())
                .triggerSource(run.getTriggerSource())
                .operatorId(run.getOperatorId())
                .operatorType(run.getOperatorType())
                .sessionId(run.getSessionId())
                .status(run.getStatus())
                .modelIdUsed(run.getModelIdUsed())
                .modelNameUsed(run.getModelNameUsed())
                .promptTokens(run.getPromptTokens())
                .completionTokens(run.getCompletionTokens())
                .totalTokens(run.getTotalTokens())
                .toolCallCount(run.getToolCallCount())
                .toolDeniedCount(run.getToolDeniedCount())
                .repairAttempts(run.getRepairAttempts())
                .costMs(run.getCostMs())
                .errorMessage(run.getErrorMessage())
                .startedAt(run.getStartedAt())
                .endedAt(run.getEndedAt())
                .build();
    }

    /**
     * 持久化对象转实体。
     */
    private AgentRun toEntity(AgentRunPO po) {
        if (po == null) {
            return null;
        }
        return AgentRun.builder()
                .runId(po.getRunId())
                .agentId(po.getAgentId())
                .agentCode(po.getAgentCode())
                .agentVersionId(po.getAgentVersionId())
                .runType(po.getRunType())
                .triggerSource(po.getTriggerSource())
                .operatorId(po.getOperatorId())
                .operatorType(po.getOperatorType())
                .sessionId(po.getSessionId())
                .status(po.getStatus())
                .modelIdUsed(po.getModelIdUsed())
                .modelNameUsed(po.getModelNameUsed())
                .promptTokens(po.getPromptTokens())
                .completionTokens(po.getCompletionTokens())
                .totalTokens(po.getTotalTokens())
                .toolCallCount(po.getToolCallCount())
                .toolDeniedCount(po.getToolDeniedCount())
                .repairAttempts(po.getRepairAttempts())
                .costMs(po.getCostMs())
                .errorMessage(po.getErrorMessage())
                .startedAt(po.getStartedAt())
                .endedAt(po.getEndedAt())
                .build();
    }
}
