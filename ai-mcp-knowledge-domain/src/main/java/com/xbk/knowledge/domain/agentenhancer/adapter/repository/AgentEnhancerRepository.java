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
     * 
     * @param id 主键 ID。
     * @return 可选的增强器实体。
     */
    Optional<AgentEnhancer> findById(Long id);

    /**
     * 按编码查询记录。
     * 
     * @param agentEnhancerCode 增强器编码。
     * @return 可选的增强器实体。
     */
    Optional<AgentEnhancer> findByCode(String agentEnhancerCode);

    /**
     * 按条件分页查询记录。
     * 
     * @param query 分页查询条件。
     * @return 增强器分页列表。
     */
    List<AgentEnhancer> findPage(AgentEnhancerPageQuery query);

    /**
     * 统计符合条件的记录数量。
     * 
     * @param query 分页查询条件。
     * @return 统计数量。
     */
    long count(AgentEnhancerPageQuery query);

    /**
     * 新增记录。
     * 
     * @param agentEnhancer 待新增的增强器实体。
     * @return 已持久化的增强器实体。
     */
    AgentEnhancer insert(AgentEnhancer agentEnhancer);

    /**
     * 更新记录。
     * 
     * @param agentEnhancer 待更新的增强器实体。
     * @return 影响行数。
     */
    int update(AgentEnhancer agentEnhancer);

    /**
     * 更新启用状态。
     * 
     * @param id 主键 ID。
     * @param enabled 启用标识。
     * @return 影响行数。
     */
    int updateEnabled(Long id, Integer enabled);

    /**
     * 按主键删除记录。
     * 
     * @param id 主键 ID。
     * @return 影响行数。
     */
    int deleteById(Long id);
}
