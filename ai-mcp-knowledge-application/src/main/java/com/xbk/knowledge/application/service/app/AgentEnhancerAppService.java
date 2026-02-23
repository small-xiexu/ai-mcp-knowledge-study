package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.agentenhancer.model.entity.AgentEnhancer;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * AgentEnhancer 控制面应用服务。
 *
 * 职责：提供 AgentEnhancer 资产管理（CRUD、启停、分页）。
 *
 * @author sxie
 */
public interface AgentEnhancerAppService {

    PageResult<AgentEnhancer> queryPage(AgentEnhancerPageQuery query);

    AgentEnhancer get(Long id);

    AgentEnhancer save(AgentEnhancer agentEnhancer);

    AgentEnhancer enable(Long id);

    AgentEnhancer disable(Long id);

    void remove(Long id);
}
