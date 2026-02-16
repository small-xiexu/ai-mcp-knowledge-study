package com.xbk.knowledge.domain.repository.rag;

import com.xbk.knowledge.domain.model.entity.RagTask;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 任务仓储接口
 *
 * 职责：RAG 任务数据持久化访问
 * @author xiexu
 */
public interface RagTaskRepository {

    /**
     * 新建任务
     *
     * 为什么：持久化任务记录，便于进度追踪
     * 入参：任务实体
     * 出参：持久化后的任务
     */
    RagTask create(RagTask task);

    /**
     * 更新任务
     *
     * 为什么：更新任务状态与进度
     * 入参：任务实体
     * 出参：更新后的任务
     */
    RagTask update(RagTask task);

    /**
     * 按任务ID查询
     *
     * 为什么：按唯一任务标识获取任务状态
     * 入参：任务 ID
     * 出参：任务详情
     */
    RagTask findByTaskId(String taskId);

    /**
     * 查询任务列表
     *
     * 为什么：分页查询任务以控制响应大小
     * 入参：偏移量、条数
     * 出参：任务列表
     */
    List<RagTask> findPage(int offset, int limit);

    /**
     * 统计任务总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    long countAll();

    /**
     * 按 org 统计任务总数。
     */
    long countByOrgId(Long orgId);

    /**
     * 按 org + 状态统计任务数。
     */
    long countByOrgIdAndStatus(Long orgId, String status);

    /**
     * 统计 org 下出现过的 RAG 标签数量（distinct rag_tag）。
     */
    long countDistinctRagTagByOrgId(Long orgId);

    /**
     * 统计 org 在指定时间后失败的任务数（失败或带 error_details 的完成任务）。
     */
    long countFailedTasksSince(Long orgId, LocalDateTime since);

    /**
     * 查询指定时间后失败的任务
     *
     * 为什么：用于失败重试或告警统计
     * 入参：起始时间
     * 出参：失败任务列表
     */
    List<RagTask> findFailedTasksSince(LocalDateTime since);

    /**
     * 查询指定时间前仍处于 PROCESSING 状态的任务
     *
     * 为什么：识别超时任务以便清理或重试
     * 入参：截止时间
     * 出参：超时任务列表
     */
    List<RagTask> findProcessingTasksBefore(LocalDateTime before);

    /**
     * 删除指定时间前的已完成任务
     *
     * 为什么：定期清理历史任务，控制数据规模
     * 入参：截止时间
     * 出参：删除数量
     */
    int deleteCompletedTasksBefore(LocalDateTime before);
}
