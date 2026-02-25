package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.RagTaskPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 任务 Mapper
 *
 * 职责：数据访问适配层
 * @author sxie
 */
@Mapper
public interface IRagTaskDao {

    /**
     * 插入任务
     *
     * 落库任务记录
     * 
     * @param task 待写入的任务持久化实体。
     */
    void insertTask(@Param("task") RagTaskPO task);

    /**
     * 更新任务
     *
     * 更新任务状态与进度
     * 
     * @param task 待更新的任务持久化实体。
     */
    void updateTask(@Param("task") RagTaskPO task);

    /**
     * 按任务 ID 查询
     *
     * 获取任务当前状态
     * 
     * @param taskId 任务 ID。
     * @return 任务持久化实体。
     */
    RagTaskPO findByTaskId(@Param("taskId") String taskId);

    /**
     * 分页查询任务
     *
     * 控制单次返回数量
     * 
     * @param offset 分页偏移量。
     * @param limit 分页大小。
     * @return 任务列表。
     */
    List<RagTaskPO> findPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计任务总数
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    long countAll();

    long countByStatus(@Param("status") String status);

    long countDistinctRagTag();

    long countFailedTasksSince(@Param("since") LocalDateTime since);

    /**
     * 查询指定时间后失败的任务
     *
     * 支持失败重试或告警统计
     * 
     * @param since 起始时间（查询该时间之后失败的任务）。
     * @return 失败任务持久化对象列表。
     */
    List<RagTaskPO> findFailedTasksSince(@Param("since") LocalDateTime since);

    /**
     * 查询指定时间前仍处于 PROCESSING 状态的任务
     *
     * 识别超时任务用于清理或重试
     * 
     * @param before 截止时间（早于该时间且仍处理中）。
     * @return 超时候选任务持久化对象列表。
     */
    List<RagTaskPO> findProcessingTasksBefore(@Param("before") LocalDateTime before);

    /**
     * 删除指定时间前的已完成任务
     *
     * 定期清理历史任务
     * 
     * @param before 截止时间（早于该时间的已完成任务会被删除）。
     * @return 影响行数。
     */
    int deleteCompletedTasksBefore(@Param("before") LocalDateTime before);
}
