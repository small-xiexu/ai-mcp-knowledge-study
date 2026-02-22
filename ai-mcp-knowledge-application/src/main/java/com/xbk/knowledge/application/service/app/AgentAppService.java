package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * Agent 控制面应用服务。
 *
 * @author sxie
 */
public interface AgentAppService {

    PageResult<Agent> queryPage(AgentPageQuery query);

    Agent queryByCode(AgentCodeQuery query);

    Agent create(Agent agent);

    Agent update(Agent agent);

    void remove(AgentCodeQuery query);
}
