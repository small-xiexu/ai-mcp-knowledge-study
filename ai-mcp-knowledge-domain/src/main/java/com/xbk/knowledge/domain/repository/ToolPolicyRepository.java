package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;

import java.util.Optional;

/**
 * ToolPolicy 仓储接口。
 *
 * 职责：提供按 org + toolKey 的风险与审批门禁配置查询能力。
 *
 * @author xiexu
 */
public interface ToolPolicyRepository {

    Optional<ToolPolicy> findEnabled(Long orgId, String toolKey);
}

