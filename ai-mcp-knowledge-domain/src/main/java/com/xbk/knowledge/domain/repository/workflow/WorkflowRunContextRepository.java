package com.xbk.knowledge.domain.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRunContext;

import java.util.Optional;

/**
 * WorkflowRunContext 仓储接口。
 
  * @author xiexu
  */
public interface WorkflowRunContextRepository {

    WorkflowRunContext upsert(WorkflowRunContext ctx);

    Optional<WorkflowRunContext> findByRunId(Long orgId, String runId);

    int updateStatus(Long orgId, String runId, String status);

    int deleteByRunIds(Long orgId, java.util.List<String> runIds);
}
