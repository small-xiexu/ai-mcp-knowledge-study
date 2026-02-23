package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRun;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowRun 仓储接口。
 *
 * @author sxie
 */
public interface WorkflowRunRepository {

    /**
     * 新增记录。
     */
    void insert(WorkflowRun run);

    /**
     * 更新执行状态及异常信息。
     */
    int updateStatus(String runId, String status, String errorMessage, LocalDateTime endedAt);

    /**
     * 更新执行状态与指标数据。
     */
    void updateStatusAndMetrics(WorkflowRun run);

    /**
     * 按运行 ID 查询记录。
     */
    Optional<WorkflowRun> findByRunId(String runId);

    /**
     * 按条件查询列表。
     */
    List<WorkflowRun> list(String status, int offset, int pageSize);

    /**
     * 统计符合条件的记录数量。
     */
    long count(String status);

    /**
     * 删除 startedAt 在 cutOff 之前的运行记录（用于留存清理）。
     * 返回删除条数。
     */
    int deleteBefore(LocalDateTime cutOff, int limit);

    /**
     * 查询截止时间之前的运行 ID 列表。
     */
    List<String> listRunIdsBefore(LocalDateTime cutOff, int limit);

    /**
     * 批量删除运行记录。
     */
    int deleteByRunIds(List<String> runIds);
}
