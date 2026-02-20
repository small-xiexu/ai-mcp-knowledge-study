package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRun;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowRun 仓储接口。
 
  * @author xiexu
  */
public interface WorkflowRunRepository {

    /**
     * 方法：insert。
     */
    WorkflowRun insert(WorkflowRun run);

    /**
     * 方法：updateStatus。
     */
    int updateStatus(String runId, String status, String errorMessage, LocalDateTime endedAt);

    /**
     * 方法：updateStatusAndMetrics。
     */
    int updateStatusAndMetrics(WorkflowRun run);

    /**
     * 方法：findByRunId。
     */
    Optional<WorkflowRun> findByRunId(String runId);

    /**
     * 方法：list。
     */
    List<WorkflowRun> list(String status, int offset, int pageSize);

    /**
     * 方法：count。
     */
    long count(String status);

    /**
     * 删除 startedAt 在 cutOff 之前的运行记录（用于留存清理）。
     * 返回删除条数。
     */
    int deleteBefore(LocalDateTime cutOff, int limit);

    /**
     * 方法：listRunIdsBefore。
     */
    List<String> listRunIdsBefore(LocalDateTime cutOff, int limit);

    /**
     * 方法：deleteByRunIds。
     */
    int deleteByRunIds(List<String> runIds);
}
