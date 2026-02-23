package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.AgentEnhancerPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AgentEnhancerPO Mapper（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IAgentEnhancerDao extends BaseMapper<AgentEnhancerPO> {

    AgentEnhancerPO findById(@Param("id") Long id);

    AgentEnhancerPO findByCode(@Param("agentEnhancerCode") String agentEnhancerCode);

    List<AgentEnhancerPO> findPage(@Param("q") AgentEnhancerPageQuery query);

    long count(@Param("q") AgentEnhancerPageQuery query);

    int insertAgentEnhancer(AgentEnhancerPO agentEnhancer);

    int updateAgentEnhancer(AgentEnhancerPO agentEnhancer);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    int deleteById(@Param("id") Long id);
}
