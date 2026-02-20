package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentRunContext;

import java.util.Optional;

/**
 * AgentRunContext 仓储接口。
 *
 * 职责：保存可恢复的运行上下文快照，用于“方式B”审批通过后自动续跑。
 *
 * @author sxie
 */
public interface AgentRunContextRepository {

    /**
     * 方法：upsert。
     */
    void upsert(AgentRunContext context);

    /**
     * 方法：findByRunId。
     */
    Optional<AgentRunContext> findByRunId(String runId);

    /**
     * 方法：updateStatus。
     */
    int updateStatus(String runId, String status);
}
