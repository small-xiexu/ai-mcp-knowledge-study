package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * AgentVersion 控制面应用服务。
 *
 * @author sxie
 */
public interface AgentVersionAppService {

    PageResult<AgentVersion> queryPage(AgentVersionPageQuery query);

    AgentVersion queryById(AgentVersionIdQuery query);

    AgentVersion createDraft(AgentVersion draft);

    AgentVersion updateDraft(AgentVersion draft);

    AgentVersion publish(String agentCode, Long versionId, Long operatorId);

    AgentVersion rollback(String agentCode, Long targetVersionId, Long operatorId);
}

