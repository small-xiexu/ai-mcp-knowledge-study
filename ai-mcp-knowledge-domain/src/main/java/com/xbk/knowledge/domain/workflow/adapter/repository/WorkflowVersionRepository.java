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
     */
    Optional<WorkflowVersion> findById(WorkflowVersionIdQuery query);

    /**
     * 查询 Workflow 下的版本列表。
     */
    List<WorkflowVersion> listByWorkflowId(WorkflowVersionListQuery query);

    /**
     * 新增记录。
     */
    WorkflowVersion insert(WorkflowVersion version);

    /**
     * 按主键更新记录。
     */
    int updateById(WorkflowVersion version);

    /**
     * 查询 Workflow 当前已发布版本。
     */
    Optional<WorkflowVersion> findPublishedVersion(Long workflowId);
}

