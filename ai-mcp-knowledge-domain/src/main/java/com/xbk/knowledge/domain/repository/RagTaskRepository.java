package com.xbk.knowledge.domain.repository;

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
     * @param task 任务
     * @return 任务
     */
    RagTask create(RagTask task);

    /**
     * 更新任务
     *
     * @param task 任务
     * @return 任务
     */
    RagTask update(RagTask task);

    /**
     * 按任务ID查询
     *
     * @param taskId 任务ID
     * @return 任务
     */
    RagTask findByTaskId(String taskId);

    /**
     * 查询任务列表
     *
     * @param offset 偏移量
     * @param limit  条数
     * @return 任务列表
     */
    List<RagTask> findPage(int offset, int limit);

    /**
     * 统计任务总数
     *
     * @return 总数
     */
    long countAll();

    /**
     * 查询指定时间后失败的任务
     *
     * @param since 起始时间
     * @return 失败任务列表
     */
    List<RagTask> findFailedTasksSince(LocalDateTime since);

    /**
     * 查询指定时间前仍处于 PROCESSING 状态的任务
     *
     * @param before 截止时间
     * @return 超时任务列表
     */
    List<RagTask> findProcessingTasksBefore(LocalDateTime before);

    /**
     * 删除指定时间前的已完成任务
     *
     * @param before 截止时间
     * @return 删除数量
     */
    int deleteCompletedTasksBefore(LocalDateTime before);
}
