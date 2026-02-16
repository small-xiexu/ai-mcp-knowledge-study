package com.xbk.knowledge.domain.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNodeRun;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowNodeRun 仓储接口。
 
  * @author xiexu
  */
public interface WorkflowNodeRunRepository {

    WorkflowNodeRun insert(WorkflowNodeRun nodeRun);

    int updateById(WorkflowNodeRun nodeRun);

    Optional<WorkflowNodeRun> findByRunIdAndNodeKey(Long orgId, String runId, String nodeKey);

    List<WorkflowNodeRun> listByRunId(Long orgId, String runId);

    int incrementToolCallCount(Long orgId, String runId, String nodeKey, int delta);

    int incrementToolDeniedCount(Long orgId, String runId, String nodeKey, int delta);

    int deleteByRunIds(Long orgId, List<String> runIds);
}
