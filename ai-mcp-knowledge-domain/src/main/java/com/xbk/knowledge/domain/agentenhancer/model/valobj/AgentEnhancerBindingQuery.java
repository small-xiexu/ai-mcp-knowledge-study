package com.xbk.knowledge.domain.agentenhancer.model.valobj;

/**
 * AgentEnhancer 绑定查询条件。
 *
 * @param bindType 绑定类型AGENT_VERSION/WORKFLOW_VERSION
 * @param bindTargetId 绑定目标 ID
 *
 * @author sxie
 */
public record AgentEnhancerBindingQuery(String bindType,
                                  Long bindTargetId) {
}

