package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Agent 仓储接口。
 */
public interface AgentRepository {

    Optional<Agent> findByCode(AgentCodeQuery query);

    boolean existsByCode(AgentCodeQuery query);

    Agent insert(Agent agent);

    int updateByCode(Agent agent);

    List<Agent> findPage(AgentPageQuery query);

    long count(AgentPageQuery query);
}

