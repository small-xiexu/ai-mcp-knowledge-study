package com.xbk.knowledge.domain.agent.service;

import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * Agent 领域服务接口。
 *
 * @author sxie
 */
public interface IAgentService {

    PageResult<Agent> queryPage(AgentPageQuery query);

    Agent queryByCode(AgentCodeQuery query);

    Agent create(Agent agent);

    Agent update(Agent agent);
}

