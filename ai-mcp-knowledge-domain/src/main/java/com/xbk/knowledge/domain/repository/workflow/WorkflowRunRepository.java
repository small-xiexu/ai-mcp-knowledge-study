package com.xbk.knowledge.domain.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRun;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowRun 仓储接口。
 
  * @author xiexu
  */
public interface WorkflowRunRepository {

    WorkflowRun insert(WorkflowRun run);

    int updateStatus(Long orgId, String runId, String status, String errorMessage, LocalDateTime endedAt);

    int updateStatusAndMetrics(WorkflowRun run);

    Optional<WorkflowRun> findByRunId(Long orgId, String runId);

    List<WorkflowRun> list(Long orgId, String status, int offset, int pageSize);

    long count(Long orgId, String status);

    /**
     * 删除 startedAt 在 cutOff 之前的运行记录（用于留存清理）。
     * 返回删除条数。
     */
    int deleteBefore(Long orgId, LocalDateTime cutOff, int limit);

    List<String> listRunIdsBefore(Long orgId, LocalDateTime cutOff, int limit);

    int deleteByRunIds(Long orgId, List<String> runIds);
}
