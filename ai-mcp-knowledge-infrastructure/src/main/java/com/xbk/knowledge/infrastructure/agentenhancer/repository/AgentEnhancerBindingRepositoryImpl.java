package com.xbk.knowledge.infrastructure.agentenhancer.repository;

import com.xbk.knowledge.domain.agentenhancer.model.entity.AgentEnhancerBinding;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingQuery;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingView;
import com.xbk.knowledge.domain.agentenhancer.adapter.repository.AgentEnhancerBindingRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IAgentEnhancerBindingDao;
import com.xbk.knowledge.infrastructure.dao.po.AgentEnhancerBindingPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * AgentEnhancer 绑定仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class AgentEnhancerBindingRepositoryImpl implements AgentEnhancerBindingRepository {

    /**
     * AgentEnhancer 绑定数据访问对象。
     */
    private final IAgentEnhancerBindingDao mapper;

    /**
     * 查询指定绑定目标下的 AgentEnhancer 绑定关系。
     * 
     * @param query 绑定查询条件（绑定类型 + 绑定目标 ID）
     * @return 绑定关系列表
     */
    @Override
    public List<AgentEnhancerBinding> listBindings(AgentEnhancerBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.listBindings(query), AgentEnhancerBinding.class);
    }

    /**
     * 查询绑定视图数据（含 AgentEnhancer 基础信息）。
     * 
     * @param query 绑定查询条件（绑定类型 + 绑定目标 ID）
     * @return 绑定视图列表
     */
    @Override
    public List<AgentEnhancerBindingView> listBindingViews(AgentEnhancerBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return Collections.emptyList();
        }
        return mapper.listBindingViews(query);
    }

    /**
     * 删除指定绑定目标下的全部 AgentEnhancer 绑定关系。
     * 
     * @param query 绑定查询条件（绑定类型 + 绑定目标 ID）
     * @return 删除影响行数
     */
    @Override
    public int deleteByTarget(AgentEnhancerBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return 0;
        }
        return mapper.deleteByTarget(query);
    }

    /**
     * 新增一条 AgentEnhancer 绑定关系。
     * 
     * @param binding 绑定实体
     * @return 新增影响行数
     */
    @Override
    public int insertBinding(AgentEnhancerBinding binding) {
        if (binding == null || !StringUtils.hasText(binding.getBindType())
                || binding.getBindTargetId() == null || binding.getAgentEnhancerId() == null) {
            return 0;
        }
        return mapper.insertBinding(BeanMappingUtils.map(binding, AgentEnhancerBindingPO.class));
    }

    /**
     * 按 AgentEnhancer 主键删除其全部绑定关系。
     * 
     * @param agentEnhancerId AgentEnhancer 主键
     * @return 删除影响行数
     */
    @Override
    public int deleteByAgentEnhancerId(Long agentEnhancerId) {
        if (agentEnhancerId == null) {
            return 0;
        }
        return mapper.deleteByAgentEnhancerId(agentEnhancerId);
    }
}
