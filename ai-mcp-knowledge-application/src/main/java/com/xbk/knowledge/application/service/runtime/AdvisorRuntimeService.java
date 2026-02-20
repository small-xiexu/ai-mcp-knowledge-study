package com.xbk.knowledge.application.service.runtime;

import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

/**
 * Advisor 运行时装配服务。
 *
 * 职责：按 AgentVersion/WorkflowVersion 绑定配置解析并生成可用于 ChatClient 的 CallAdvisor 列表。
 *
 * @author sxie
 */
public interface AdvisorRuntimeService {

    CallAdvisor[] resolveForAgentVersion(Long agentVersionId, String runId, Long sessionId);

    CallAdvisor[] resolveForWorkflowVersion(Long workflowVersionId, String runId, Long sessionId);

    /**
     * 绑定配置变更后调用，用于驱逐缓存（可选）。
     */
    void evictBindingCache(String bindType, Long bindTargetId);

    /**
     * Advisor 资产变更后调用，用于驱逐所有绑定缓存（可选）。
     */
    void evictAll();
}
