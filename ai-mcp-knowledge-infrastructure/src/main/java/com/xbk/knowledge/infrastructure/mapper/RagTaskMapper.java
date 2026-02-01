package com.xbk.knowledge.infrastructure.mapper;

import com.xbk.knowledge.domain.model.entity.RagTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
