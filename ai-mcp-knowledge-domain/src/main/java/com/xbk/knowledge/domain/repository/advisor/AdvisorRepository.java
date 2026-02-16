package com.xbk.knowledge.domain.repository.advisor;

import com.xbk.knowledge.domain.model.entity.advisor.Advisor;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Advisor 资产仓储接口。
 *
 * 职责：提供 Advisor 资产 CRUD 与分页查询能力（按 org 隔离）。
 
  * @author xiexu
  */
public interface AdvisorRepository {

    Optional<Advisor> findById(Long orgId, Long id);

    Optional<Advisor> findByCode(Long orgId, String advisorCode);

    List<Advisor> findPage(AdvisorPageQuery query);

    long count(AdvisorPageQuery query);

    Advisor insert(Advisor advisor);

    int update(Advisor advisor);

    int updateEnabled(Long orgId, Long id, Integer enabled);

    int deleteById(Long orgId, Long id);
}

