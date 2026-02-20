package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionListQuery;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowVersion 仓储接口。
 
  * @author xiexu
  */
public interface WorkflowVersionRepository {

    /**
     * 方法：findById。
     */
    Optional<WorkflowVersion> findById(WorkflowVersionIdQuery query);

    /**
     * 方法：listByWorkflowId。
     */
    List<WorkflowVersion> listByWorkflowId(WorkflowVersionListQuery query);

    /**
     * 方法：insert。
     */
    WorkflowVersion insert(WorkflowVersion version);

    /**
     * 方法：updateById。
     */
    int updateById(WorkflowVersion version);

    /**
     * 方法：findPublishedVersion。
     */
    Optional<WorkflowVersion> findPublishedVersion(Long workflowId);
}

