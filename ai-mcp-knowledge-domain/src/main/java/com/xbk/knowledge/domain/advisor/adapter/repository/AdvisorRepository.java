package com.xbk.knowledge.domain.advisor.adapter.repository;

import com.xbk.knowledge.domain.advisor.model.entity.Advisor;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Advisor 资产仓储接口。
 *
 * 职责：提供 Advisor 资产 CRUD 与分页查询能力。
 *
 * @author sxie
 */
public interface AdvisorRepository {

    /**
     * 按主键查询记录。
     */
    Optional<Advisor> findById(Long id);

    /**
     * 按编码查询记录。
     */
    Optional<Advisor> findByCode(String advisorCode);

    /**
     * 按条件分页查询记录。
     */
    List<Advisor> findPage(AdvisorPageQuery query);

    /**
     * 统计符合条件的记录数量。
     */
    long count(AdvisorPageQuery query);

    /**
     * 新增记录。
     */
    Advisor insert(Advisor advisor);

    /**
     * 更新记录。
     */
    int update(Advisor advisor);

    /**
     * 更新启用状态。
     */
    int updateEnabled(Long id, Integer enabled);

    /**
     * 按主键删除记录。
     */
    int deleteById(Long id);
}
