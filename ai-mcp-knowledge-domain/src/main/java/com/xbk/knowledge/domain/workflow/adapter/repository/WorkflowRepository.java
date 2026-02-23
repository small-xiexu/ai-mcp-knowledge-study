package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowCodeQuery;

import java.util.List;
import java.util.Optional;

/**
 * Workflow 资产仓储接口。
 *
 * @author sxie
 */
public interface WorkflowRepository {

    /**
     * 按主键查询记录。
     */
    Optional<Workflow> findById(IdQuery query);

    /**
     * 按编码查询记录。
     */
    Optional<Workflow> findByCode(WorkflowCodeQuery query);

    /**
     * 新增记录。
     */
    Workflow insert(Workflow workflow);

    /**
     * 按主键更新记录。
     */
    int updateById(Workflow workflow);

    /**
     * 按条件查询列表。
     */
    List<Workflow> list(String keyword, int offset, int pageSize);

    /**
     * 统计符合条件的记录数量。
     */
    long count(String keyword);
}

