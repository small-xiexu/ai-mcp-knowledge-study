package com.xbk.knowledge.domain.service.agent;

import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * Agent 领域服务接口。
 
  * @author xiexu
  */
public interface IAgentService {

    PageResult<Agent> queryPage(AgentPageQuery query);

    Agent queryByCode(AgentCodeQuery query);

    Agent create(Agent agent);

    Agent update(Agent agent);
}

