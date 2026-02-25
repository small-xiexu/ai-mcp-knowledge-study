package com.xbk.knowledge.domain.agentenhancer.adapter.repository;

import com.xbk.knowledge.domain.agentenhancer.model.entity.AgentEnhancerBinding;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingQuery;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingView;

import java.util.List;

/**
 * AgentEnhancer 绑定仓储接口。
 *
 * 职责：
 * - 按 bindType/bindTargetId 管理 AgentEnhancer 绑定与排序
 * - 提供运行时装配需要的 join 视图查询
 *
 * @author sxie
 */
public interface AgentEnhancerBindingRepository {

    /**
     * 查询 AgentEnhancer 绑定关系列表。
     * 
     * @param query 绑定关系查询条件。
     * @return 绑定关系列表。
     */
    List<AgentEnhancerBinding> listBindings(AgentEnhancerBindingQuery query);

    /**
     * 查询 AgentEnhancer 绑定视图列表（含运行时装配信息）。
     * 
     * @param query 绑定关系查询条件。
     * @return 绑定视图列表。
     */
    List<AgentEnhancerBindingView> listBindingViews(AgentEnhancerBindingQuery query);

    /**
     * 删除指定绑定目标下的 AgentEnhancer 绑定。
     * 
     * @param query 绑定目标查询条件。
     * @return 影响行数。
     */
    int deleteByTarget(AgentEnhancerBindingQuery query);

    /**
     * 新增 AgentEnhancer 绑定关系。
     * 
     * @param binding 待新增的绑定关系实体。
     * @return 影响行数。
     */
    int insertBinding(AgentEnhancerBinding binding);

    /**
     * 删除指定 AgentEnhancer 的全部绑定关系。
     * 
     * @param agentEnhancerId 增强器 ID。
     * @return 影响行数。
     */
    int deleteByAgentEnhancerId(Long agentEnhancerId);
}
