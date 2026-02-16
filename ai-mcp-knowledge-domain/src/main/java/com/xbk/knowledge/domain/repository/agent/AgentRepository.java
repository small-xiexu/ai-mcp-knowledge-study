package com.xbk.knowledge.domain.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Agent 仓储接口。
 
  * @author xiexu
  */
public interface AgentRepository {

    Optional<Agent> findByCode(AgentCodeQuery query);

    boolean existsByCode(AgentCodeQuery query);

    Agent insert(Agent agent);

    int updateByCode(Agent agent);

    List<Agent> findPage(AgentPageQuery query);

    long count(AgentPageQuery query);

    /**
     * 统计当前 org 下“已发布版本”的 Agent 数量。
     *
     * 规则：current_published_version_id 非空视为已发布。
     */
    long countPublishedByOrgId(Long orgId);
}
