package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.advisor.Advisor;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * Advisor 控制面应用服务。
 *
 * 职责：提供 Advisor 资产管理（CRUD、启停、分页）。
 
  * @author xiexu
  */
public interface AdvisorAppService {

    PageResult<Advisor> queryPage(AdvisorPageQuery query);

    Advisor get(Long orgId, Long id);

    Advisor save(Advisor advisor);

    Advisor enable(Long orgId, Long id);

    Advisor disable(Long orgId, Long id);

    void remove(Long orgId, Long id);
}

