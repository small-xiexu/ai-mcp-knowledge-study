package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionListQuery;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowVersion 仓储接口。
 *
 * @author sxie
 */
public interface WorkflowVersionRepository {

    /**
     * 按主键查询记录。
     * 
     * @param query 主键查询条件。
     * @return 可选的工作流版本实体。
     */
    Optional<WorkflowVersion> findById(WorkflowVersionIdQuery query);

    /**
     * 查询 Workflow 下的版本列表。
     * 
     * @param query 工作流版本列表查询条件。
     * @return 工作流版本列表。
     */
    List<WorkflowVersion> listByWorkflowId(WorkflowVersionListQuery query);

    /**
     * 新增记录。
     * 
     * @param version 待新增的工作流版本实体。
     * @return 已持久化的工作流版本实体。
     */
    WorkflowVersion insert(WorkflowVersion version);

    /**
     * 按主键更新记录。
     * 
     * @param version 待更新的工作流版本实体。
     * @return 影响行数。
     */
    int updateById(WorkflowVersion version);

    /**
     * 查询 Workflow 当前已发布版本。
     * 
     * @param workflowId 工作流 ID。
     * @return 可选的已发布工作流版本实体。
     */
    Optional<WorkflowVersion> findPublishedVersion(Long workflowId);
}
