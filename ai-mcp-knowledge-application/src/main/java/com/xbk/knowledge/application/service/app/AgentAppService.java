package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * Agent 控制面应用服务。
 
  * @author xiexu
  */
public interface AgentAppService {

    PageResult<Agent> queryPage(AgentPageQuery query);

    Agent queryByCode(AgentCodeQuery query);

    Agent create(Agent agent);

    Agent update(Agent agent);
}

