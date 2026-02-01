package com.xbk.knowledge.infrastructure.mapper;

import com.xbk.knowledge.domain.model.entity.RagTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 任务 Mapper
 *
 * 职责：数据访问适配层
 * @author xiexu
 */
@Mapper
public interface RagTaskMapper {

    /**
     * 插入任务
     *
     * @param task 任务
     */
    void insertTask(@Param("task") RagTask task);

    /**
     * 更新任务
     *
     * @param task 任务
     */
    void updateTask(@Param("task") RagTask task);

    /**
     * 按任务ID查询
     *
     * @param taskId 任务ID
     * @return 任务
     */
    RagTask findByTaskId(@Param("taskId") String taskId);

    /**
     * 分页查询任务
     *
     * @param offset 偏移量
     * @param limit  条数
     * @return 任务列表
     */
    List<RagTask> findPage(@Param("offset") int offset, @Param("limit") int limit);

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
    List<RagTask> findFailedTasksSince(@Param("since") LocalDateTime since);

    /**
     * 查询指定时间前仍处于 PROCESSING 状态的任务
     *
     * @param before 截止时间
     * @return 超时任务列表
     */
    List<RagTask> findProcessingTasksBefore(@Param("before") LocalDateTime before);

    /**
     * 删除指定时间前的已完成任务
     *
     * @param before 截止时间
     * @return 删除数量
     */
    int deleteCompletedTasksBefore(@Param("before") LocalDateTime before);
}
