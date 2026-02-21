package com.xbk.knowledge.domain.advisor.model.valobj;

/**
 * Advisor 绑定查询条件。
 *
 * @param bindType 绑定类型：AGENT_VERSION/WORKFLOW_VERSION
 * @param bindTargetId 绑定目标 ID
 *
 * @author sxie
 */
public record AdvisorBindingQuery(String bindType,
                                  Long bindTargetId) {
}

