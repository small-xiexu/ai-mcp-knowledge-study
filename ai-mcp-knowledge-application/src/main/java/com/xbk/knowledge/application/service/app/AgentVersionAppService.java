package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * AgentVersion 控制面应用服务。
 */
public interface AgentVersionAppService {

    PageResult<AgentVersion> queryPage(AgentVersionPageQuery query);

    AgentVersion queryById(AgentVersionIdQuery query);

    AgentVersion createDraft(AgentVersion draft);

    AgentVersion updateDraft(AgentVersion draft);

    AgentVersion publish(Long orgId, String agentCode, Long versionId, Long operatorId);

    AgentVersion rollback(Long orgId, String agentCode, Long targetVersionId, Long operatorId);
}

