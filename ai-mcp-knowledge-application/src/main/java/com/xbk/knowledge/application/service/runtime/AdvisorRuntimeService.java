package com.xbk.knowledge.application.service.runtime;

import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

/**
 * Advisor 运行时装配服务。
 *
 * 职责：按 AgentVersion/WorkflowVersion 绑定配置解析并生成可用于 ChatClient 的 CallAdvisor 列表。
 
  * @author xiexu
  */
public interface AdvisorRuntimeService {

    CallAdvisor[] resolveForAgentVersion(Long orgId, Long agentVersionId, String runId, Long sessionId);

    CallAdvisor[] resolveForWorkflowVersion(Long orgId, Long workflowVersionId, String runId, Long sessionId);

    /**
     * 绑定配置变更后调用，用于驱逐缓存（可选）。
     */
    void evictBindingCache(Long orgId, String bindType, Long bindTargetId);

    /**
     * Advisor 资产变更后调用，用于驱逐该 org 下的所有绑定缓存（可选）。
     */
    void evictAll(Long orgId);
}
