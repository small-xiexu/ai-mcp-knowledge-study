package com.xbk.knowledge.domain.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.AgentRunContext;

import java.util.Optional;

/**
 * AgentRunContext 仓储接口。
 *
 * 职责：保存可恢复的运行上下文快照，用于“方式B”审批通过后自动续跑。
 *
 * @author xiexu
 */
public interface AgentRunContextRepository {

    int upsert(AgentRunContext context);

    Optional<AgentRunContext> findByRunId(Long orgId, String runId);

    int updateStatus(Long orgId, String runId, String status);
}

