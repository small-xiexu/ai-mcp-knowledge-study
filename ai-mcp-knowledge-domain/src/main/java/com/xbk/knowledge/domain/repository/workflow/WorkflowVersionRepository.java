package com.xbk.knowledge.domain.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowVersion;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionListQuery;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowVersion 仓储接口。
 
  * @author xiexu
  */
public interface WorkflowVersionRepository {

    Optional<WorkflowVersion> findById(WorkflowVersionIdQuery query);

    List<WorkflowVersion> listByWorkflowId(WorkflowVersionListQuery query);

    WorkflowVersion insert(WorkflowVersion version);

    int updateById(WorkflowVersion version);

    Optional<WorkflowVersion> findPublishedVersion(Long orgId, Long workflowId);
}

