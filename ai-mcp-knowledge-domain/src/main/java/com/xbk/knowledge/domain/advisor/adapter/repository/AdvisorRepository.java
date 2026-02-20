package com.xbk.knowledge.domain.advisor.adapter.repository;

import com.xbk.knowledge.domain.advisor.model.entity.Advisor;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Advisor 资产仓储接口。
 *
 * 职责：提供 Advisor 资产 CRUD 与分页查询能力（按 scope 隔离）。
 *
 * @author sxie
 */
public interface AdvisorRepository {

    /**
     * 方法：findById。
     */
    Optional<Advisor> findById(Long id);

    /**
     * 方法：findByCode。
     */
    Optional<Advisor> findByCode(String advisorCode);

    /**
     * 方法：findPage。
     */
    List<Advisor> findPage(AdvisorPageQuery query);

    /**
     * 方法：count。
     */
    long count(AdvisorPageQuery query);

    /**
     * 方法：insert。
     */
    Advisor insert(Advisor advisor);

    /**
     * 方法：update。
     */
    int update(Advisor advisor);

    /**
     * 方法：updateEnabled。
     */
    int updateEnabled(Long id, Integer enabled);

    /**
     * 方法：deleteById。
     */
    int deleteById(Long id);
}

