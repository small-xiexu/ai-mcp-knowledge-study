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
     * 
     * @param run 工作流运行记录。
     */
    void insert(WorkflowRun run);

    /**
     * 更新执行状态及异常信息。
     * 
     * @param runId 运行 ID。
     * @param status 状态值。
     * @param errorMessage 错误信息。
     * @param endedAt 结束时间。
     * @return 影响行数。
     */
    int updateStatus(String runId, String status, String errorMessage, LocalDateTime endedAt);

    /**
     * 更新执行状态与指标数据。
     * 
     * @param run 包含状态与指标的运行记录。
     */
    void updateStatusAndMetrics(WorkflowRun run);

    /**
     * 按运行 ID 查询记录。
     * 
     * @param runId 运行 ID。
     * @return 可选的工作流运行记录。
     */
    Optional<WorkflowRun> findByRunId(String runId);

    /**
     * 按条件查询列表。
     * 
     * @param status 状态值。
     * @param offset 分页偏移量。
     * @param pageSize 分页大小。
     * @return 工作流运行记录列表。
     */
    List<WorkflowRun> list(String status, int offset, int pageSize);

    /**
     * 统计符合条件的记录数量。
     * 
     * @param status 状态值。
     * @return 统计数量。
     */
    long count(String status);

    /**
     * 删除 startedAt 在 cutOff 之前的运行记录（用于留存清理）。
     * 
     * @param cutOff 截止时间（早于该时间的记录会被清理）。
     * @param limit 单次删除上限。
     * @return 删除记录数。
     */
    int deleteBefore(LocalDateTime cutOff, int limit);

    /**
     * 查询截止时间之前的运行 ID 列表。
     * 
     * @param cutOff 截止时间。
     * @param limit 返回数量上限。
     * @return 运行 ID 列表。
     */
    List<String> listRunIdsBefore(LocalDateTime cutOff, int limit);

    /**
     * 批量删除运行记录。
     * 
     * @param runIds 待删除运行 ID 列表。
     * @return 影响行数。
     */
    int deleteByRunIds(List<String> runIds);
}
