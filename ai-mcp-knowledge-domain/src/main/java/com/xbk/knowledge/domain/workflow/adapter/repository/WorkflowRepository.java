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
     * 
     * @param query 主键查询条件。
     * @return 可选的工作流实体。
     */
    Optional<Workflow> findById(IdQuery query);

    /**
     * 按编码查询记录。
     * 
     * @param query 工作流编码查询条件。
     * @return 可选的工作流实体。
     */
    Optional<Workflow> findByCode(WorkflowCodeQuery query);

    /**
     * 新增记录。
     * 
     * @param workflow 待新增的工作流实体。
     * @return 已持久化的工作流实体。
     */
    Workflow insert(Workflow workflow);

    /**
     * 按主键更新记录。
     * 
     * @param workflow 待更新的工作流实体。
     * @return 影响行数。
     */
    int updateById(Workflow workflow);

    /**
     * 按条件查询列表。
     * 
     * @param keyword 关键字。
     * @param offset 分页偏移量。
     * @param pageSize 分页大小。
     * @return 工作流分页列表。
     */
    List<Workflow> list(String keyword, int offset, int pageSize);

    /**
     * 统计符合条件的记录数量。
     * 
     * @param keyword 关键字。
     * @return 统计数量。
     */
    long count(String keyword);
}
