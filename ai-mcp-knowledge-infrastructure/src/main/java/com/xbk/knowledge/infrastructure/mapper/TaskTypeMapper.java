package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.TaskType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务类型 Mapper
 * 统一通过 XML 承载 SQL
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 * @author xiexu
 */
@Mapper
public interface TaskTypeMapper extends BaseMapper<TaskType> {

    /**
     * 新增任务类型
     *
     * @param taskType 任务类型
     * @return 影响行数
     */
    int insertTaskType(TaskType taskType);

    /**
     * 更新任务类型
     *
     * @param taskType 任务类型
     * @return 影响行数
     */
    int updateTaskType(TaskType taskType);

    /**
     * 删除任务类型
     *
     * @param id 任务类型ID
     * @return 影响行数
     */
    int deleteTaskTypeById(@Param("id") Long id);

    /**
     * 根据ID查询任务类型
     *
     * @param id 任务类型ID
     * @return 任务类型
     */
    TaskType findById(@Param("id") Long id);

    /**
     * 根据编码查询任务类型
     *
     * @param taskCode 任务编码
     * @return 任务类型
     */
    TaskType findByTaskCode(@Param("taskCode") String taskCode);

    /**
     * 查询任务类型分页数据
     *
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 任务类型列表
     */
    List<TaskType> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计任务类型总数
     *
     * @return 总数
     */
    long countAll();
}
