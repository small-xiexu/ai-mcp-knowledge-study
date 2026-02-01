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
     * 为什么：落库任务记录
     * 入参：任务
     * 出参：无
     */
    void insertTask(@Param("task") RagTask task);

    /**
     * 更新任务
     *
     * 为什么：更新任务状态与进度
     * 入参：任务
     * 出参：无
     */
    void updateTask(@Param("task") RagTask task);

    /**
     * 按任务ID查询
     *
     * 为什么：获取任务当前状态
     * 入参：任务ID
     * 出参：任务
     */
    RagTask findByTaskId(@Param("taskId") String taskId);

    /**
     * 分页查询任务
     *
     * 为什么：控制单次返回数量
     * 入参：偏移量、条数
     * 出参：任务列表
     */
    List<RagTask> findPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计任务总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    long countAll();

    /**
     * 查询指定时间后失败的任务
     *
     * 为什么：支持失败重试或告警统计
     * 入参：起始时间
     * 出参：失败任务列表
     */
    List<RagTask> findFailedTasksSince(@Param("since") LocalDateTime since);

    /**
     * 查询指定时间前仍处于 PROCESSING 状态的任务
     *
     * 为什么：识别超时任务用于清理或重试
     * 入参：截止时间
     * 出参：超时任务列表
     */
    List<RagTask> findProcessingTasksBefore(@Param("before") LocalDateTime before);

    /**
     * 删除指定时间前的已完成任务
     *
     * 为什么：定期清理历史任务
     * 入参：截止时间
     * 出参：删除数量
     */
    int deleteCompletedTasksBefore(@Param("before") LocalDateTime before);
}
