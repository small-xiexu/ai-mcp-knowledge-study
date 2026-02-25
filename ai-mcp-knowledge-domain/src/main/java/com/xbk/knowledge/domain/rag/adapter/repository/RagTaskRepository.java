package com.xbk.knowledge.domain.rag.adapter.repository;

import com.xbk.knowledge.domain.rag.model.entity.RagTask;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 任务仓储接口
 *
 * 职责：RAG 任务数据持久化访问
 * @author sxie
 */
public interface RagTaskRepository {

    /**
     * 新建任务。
     * <p>
     * 持久化任务记录，便于进度追踪。
     * 
     * @param task 待创建的任务实体。
     * @return 已持久化的任务实体。
     */
    RagTask create(RagTask task);

    /**
     * 更新任务。
     * <p>
     * 更新任务状态与进度。
     * 
     * @param task 待更新的任务实体。
     * @return 更新后的任务实体。
     */
    RagTask update(RagTask task);

    /**
     * 按任务 ID 查询。
     * <p>
     * 按唯一任务标识获取任务状态。
     * 
     * @param taskId 任务 ID。
     * @return 任务实体。
     */
    RagTask findByTaskId(String taskId);

    /**
     * 查询任务列表。
     * <p>
     * 分页查询任务以控制响应大小。
     * 
     * @param offset 分页偏移量。
     * @param limit 分页大小。
     * @return 任务分页数据列表。
     */
    List<RagTask> findPage(int offset, int limit);

    /**
     * 统计任务总数。
     * <p>
     * 为分页展示提供总记录数。
     * 
     * @return 统计数量。
     */
    long countAll();

    /**
     * 按状态统计任务数。
     * 
     * @param status 状态值。
     * @return 统计数量。
     */
    long countByStatus(String status);

    /**
     * 统计出现过的 RAG 标签数量（distinct rag_tag）。
     * 
     * @return 统计数量。
     */
    long countDistinctRagTag();

    /**
     * 统计指定时间后失败的任务数（失败或带 error_details 的完成任务）。
     * 
     * @param since 起始时间（统计该时间之后的失败任务）。
     * @return 统计数量。
     */
    long countFailedTasksSince(LocalDateTime since);

    /**
     * 查询指定时间后失败的任务。
     * <p>
     * 用于失败重试或告警统计。
     * 
     * @param since 起始时间（查询该时间之后失败的任务）。
     * @return 失败任务列表。
     */
    List<RagTask> findFailedTasksSince(LocalDateTime since);

    /**
     * 查询指定时间前仍处于 PROCESSING 状态的任务。
     * <p>
     * 识别超时任务以便清理或重试。
     * 
     * @param before 截止时间（早于该时间且仍处理中）。
     * @return 超时候选任务列表。
     */
    List<RagTask> findProcessingTasksBefore(LocalDateTime before);

    /**
     * 删除指定时间前的已完成任务。
     * <p>
     * 定期清理历史任务，控制数据规模。
     * 
     * @param before 截止时间（早于该时间的已完成任务会被删除）。
     * @return 影响行数。
     */
    int deleteCompletedTasksBefore(LocalDateTime before);
}
