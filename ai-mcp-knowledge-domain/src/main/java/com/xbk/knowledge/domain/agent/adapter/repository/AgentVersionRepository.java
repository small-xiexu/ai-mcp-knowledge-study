package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * AgentVersion 仓储接口。
 *
 * @author sxie
 */
public interface AgentVersionRepository {

    /**
     * 方法：findById。
     */
    Optional<AgentVersion> findById(AgentVersionIdQuery query);

    /**
     * 方法：findPage。
     */
    List<AgentVersion> findPage(AgentVersionPageQuery query);

    /**
     * 方法：count。
     */
    long count(AgentVersionPageQuery query);

    /**
     * 方法：findMaxVersionNo。
     */
    Integer findMaxVersionNo(Long agentId);

    /**
     * 方法：existsByAgentIdAndVersionNo。
     */
    boolean existsByAgentIdAndVersionNo(Long agentId, Integer versionNo);

    /**
     * 方法：insert。
     */
    AgentVersion insert(AgentVersion version);

    /**
     * 方法：updateDraft。
     */
    int updateDraft(AgentVersion version);

    /**
     * 方法：publish。
     */
    int publish(Long id,
                Integer promptTemplateVersionNo,
                String templateParamsJson,
                String systemPromptSnapshot,
                Long updatedBy);

    /**
     * 方法：updateState。
     */
    int updateState(Long id, String fromState, String toState);
}
