package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Agent 仓储接口。
 *
 * @author sxie
 */
public interface AgentRepository {

    /**
     * 按编码查询记录。
     */
    Optional<Agent> findByCode(AgentCodeQuery query);

    /**
     * 判断编码是否已存在。
     */
    boolean existsByCode(AgentCodeQuery query);

    /**
     * 新增记录。
     */
    Agent insert(Agent agent);

    /**
     * 按编码更新记录。
     */
    int updateByCode(Agent agent);

    /**
     * 按编码删除记录。
     */
    int deleteByCode(AgentCodeQuery query);

    /**
     * 按条件分页查询记录。
     */
    List<Agent> findPage(AgentPageQuery query);

    /**
     * 统计符合条件的记录数量。
     */
    long count(AgentPageQuery query);

    /**
     * 统计“已发布版本”的 Agent 数量。
     *
     * 规则：current_published_version_id 非空视为已发布。
     */
    long countPublished();
}
