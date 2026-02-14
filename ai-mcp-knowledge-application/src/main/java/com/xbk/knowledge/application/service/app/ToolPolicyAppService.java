package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import com.xbk.knowledge.domain.model.vo.tool.ToolPolicyPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * ToolPolicy 应用服务。
 *
 * 职责：对外提供工具风险策略（toolKey -> risk/approval）的控制面用例编排。
 *
 * @author xiexu
 */
public interface ToolPolicyAppService {

    PageResult<ToolPolicy> queryPage(ToolPolicyPageQuery query);

    ToolPolicy get(Long orgId, Long id);

    ToolPolicy save(ToolPolicy policy);

    ToolPolicy enable(Long orgId, Long id);

    ToolPolicy disable(Long orgId, Long id);

    void remove(Long orgId, Long id);
}

