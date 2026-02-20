package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.application.model.preheat.PreheatResult;

/**
 * 预热应用服务。
 *
 * 职责：提供显式 preheat 入口，用于提前装配并缓存关键运行时对象（工具/Advisor/Workflow 校验等）。
 *
 * @author sxie
 */
public interface PreheatAppService {

    PreheatResult preheatAgentVersion(Long agentVersionId, boolean refreshMcp);

    PreheatResult preheatWorkflowVersion(Long workflowVersionId, boolean refreshMcp);
}

