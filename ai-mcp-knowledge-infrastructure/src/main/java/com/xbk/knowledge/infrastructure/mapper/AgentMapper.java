package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Agent Mapper（通过 XML 承载 SQL）。
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

    int insertAgent(Agent agent);

    int updateAgentByCode(Agent agent);

    Agent findByCode(AgentCodeQuery query);

    long count(AgentPageQuery query);

    List<Agent> findPage(AgentPageQuery query);
}

