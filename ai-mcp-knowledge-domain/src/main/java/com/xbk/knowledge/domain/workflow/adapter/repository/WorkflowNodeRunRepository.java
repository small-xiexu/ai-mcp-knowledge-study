package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNodeRun;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowNodeRun 仓储接口。
 
  * @author xiexu
  */
public interface WorkflowNodeRunRepository {

    /**
     * 方法：insert。
     */
    WorkflowNodeRun insert(WorkflowNodeRun nodeRun);

    /**
     * 方法：updateById。
     */
    int updateById(WorkflowNodeRun nodeRun);

    /**
     * 方法：findByRunIdAndNodeKey。
     */
    Optional<WorkflowNodeRun> findByRunIdAndNodeKey(String runId, String nodeKey);

    /**
     * 方法：listByRunId。
     */
    List<WorkflowNodeRun> listByRunId(String runId);

    /**
     * 方法：incrementToolCallCount。
     */
    int incrementToolCallCount(String runId, String nodeKey, int delta);

    /**
     * 方法：incrementToolDeniedCount。
     */
    int incrementToolDeniedCount(String runId, String nodeKey, int delta);

    /**
     * 方法：deleteByRunIds。
     */
    int deleteByRunIds(List<String> runIds);
}
