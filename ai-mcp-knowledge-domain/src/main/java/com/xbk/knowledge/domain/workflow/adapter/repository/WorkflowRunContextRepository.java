package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRunContext;

import java.util.Optional;

/**
 * WorkflowRunContext 仓储接口。
 
  * @author xiexu
  */
public interface WorkflowRunContextRepository {

    /**
     * 方法：upsert。
     */
    WorkflowRunContext upsert(WorkflowRunContext ctx);

    /**
     * 方法：findByRunId。
     */
    Optional<WorkflowRunContext> findByRunId(String runId);

    /**
     * 方法：updateStatus。
     */
    int updateStatus(String runId, String status);

    /**
     * 方法：deleteByRunIds。
     */
    int deleteByRunIds(java.util.List<String> runIds);
}
