package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.AgentEnhancerBindingPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingQuery;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AgentEnhancerBindingPO Mapper（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IAgentEnhancerBindingDao extends BaseMapper<AgentEnhancerBindingPO> {

    List<AgentEnhancerBindingPO> listBindings(@Param("q") AgentEnhancerBindingQuery query);

    List<AgentEnhancerBindingView> listBindingViews(@Param("q") AgentEnhancerBindingQuery query);

    int deleteByTarget(@Param("q") AgentEnhancerBindingQuery query);

    int insertBinding(AgentEnhancerBindingPO binding);

    int deleteByAgentEnhancerId(@Param("agentEnhancerId") Long agentEnhancerId);
}
