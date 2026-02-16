package com.xbk.knowledge.domain.model.vo.advisor;

/**
 * Advisor 绑定查询条件。
 *
 * @param orgId 组织ID
 * @param bindType 绑定类型：AGENT_VERSION/WORKFLOW_VERSION
 * @param bindTargetId 绑定目标 ID
 
  * @author xiexu
  */
public record AdvisorBindingQuery(Long orgId,
                                  String bindType,
                                  Long bindTargetId) {
}

