package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * AgentVersion 仓储接口。
 */
public interface AgentVersionRepository {

    Optional<AgentVersion> findById(AgentVersionIdQuery query);

    List<AgentVersion> findPage(AgentVersionPageQuery query);

    long count(AgentVersionPageQuery query);

    Integer findMaxVersionNo(Long agentId);

    boolean existsByAgentIdAndVersionNo(Long agentId, Integer versionNo);

    AgentVersion insert(AgentVersion version);

    int updateDraft(AgentVersion version);

    int publish(Long orgId,
                Long id,
                Integer promptTemplateVersionNo,
                String templateParamsJson,
                String systemPromptSnapshot,
                Long updatedBy);

    int updateState(Long orgId, Long id, String fromState, String toState);
}
