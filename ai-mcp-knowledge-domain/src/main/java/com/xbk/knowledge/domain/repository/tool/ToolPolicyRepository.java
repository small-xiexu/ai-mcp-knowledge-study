package com.xbk.knowledge.domain.repository.tool;

import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import com.xbk.knowledge.domain.model.vo.tool.ToolPolicyPageQuery;

import java.util.List;
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

    Optional<ToolPolicy> findById(Long orgId, Long id);

    Optional<ToolPolicy> findByToolKey(Long orgId, String toolKey);

    List<ToolPolicy> findPage(ToolPolicyPageQuery query);

    long count(ToolPolicyPageQuery query);

    ToolPolicy insert(ToolPolicy policy);

    int update(ToolPolicy policy);

    int updateEnabled(Long orgId, Long id, Integer enabled);

    int deleteById(Long orgId, Long id);
}
