package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Agent 仓储接口。
 
  * @author xiexu
  */
public interface AgentRepository {

    /**
     * 方法：findByCode。
     */
    Optional<Agent> findByCode(AgentCodeQuery query);

    /**
     * 方法：existsByCode。
     */
    boolean existsByCode(AgentCodeQuery query);

    /**
     * 方法：insert。
     */
    Agent insert(Agent agent);

    /**
     * 方法：updateByCode。
     */
    int updateByCode(Agent agent);

    /**
     * 方法：findPage。
     */
    List<Agent> findPage(AgentPageQuery query);

    /**
     * 方法：count。
     */
    long count(AgentPageQuery query);

    /**
     * 统计“已发布版本”的 Agent 数量。
     *
     * 规则：current_published_version_id 非空视为已发布。
     */
    long countPublished();
}
