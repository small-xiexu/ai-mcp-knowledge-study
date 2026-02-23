package com.xbk.knowledge.domain.agentenhancer.adapter.repository;

import com.xbk.knowledge.domain.agentenhancer.model.entity.AgentEnhancer;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * AgentEnhancer 资产仓储接口。
 *
 * 职责：提供 AgentEnhancer 资产 CRUD 与分页查询能力。
 *
 * @author sxie
 */
public interface AgentEnhancerRepository {

    /**
     * 按主键查询记录。
     */
    Optional<AgentEnhancer> findById(Long id);

    /**
     * 按编码查询记录。
     */
    Optional<AgentEnhancer> findByCode(String agentEnhancerCode);

    /**
     * 按条件分页查询记录。
     */
    List<AgentEnhancer> findPage(AgentEnhancerPageQuery query);

    /**
     * 统计符合条件的记录数量。
     */
    long count(AgentEnhancerPageQuery query);

    /**
     * 新增记录。
     */
    AgentEnhancer insert(AgentEnhancer agentEnhancer);

    /**
     * 更新记录。
     */
    int update(AgentEnhancer agentEnhancer);

    /**
     * 更新启用状态。
     */
    int updateEnabled(Long id, Integer enabled);

    /**
     * 按主键删除记录。
     */
    int deleteById(Long id);
}
