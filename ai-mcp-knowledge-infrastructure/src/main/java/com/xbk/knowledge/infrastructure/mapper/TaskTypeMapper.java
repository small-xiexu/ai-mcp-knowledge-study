package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypePageQuery;
import org.apache.ibatis.annotations.Mapper;

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
     * @param query ID 查询条件
     * @return 影响行数
     */
    int deleteTaskTypeById(IdQuery query);

    /**
     * 根据ID查询任务类型
     *
     * @param query ID 查询条件
     * @return 任务类型
     */
    TaskType findById(IdQuery query);

    /**
     * 根据编码查询任务类型
     *
     * @param query 任务编码查询条件
     * @return 任务类型
     */
    TaskType findByTaskCode(TaskTypeCodeQuery query);

    /**
     * 查询任务类型分页数据
     *
     * @param query 分页查询条件
     * @return 任务类型列表
     */
    List<TaskType> findPage(TaskTypePageQuery query);

    /**
     * 统计任务类型总数
     *
     * @return 总数
     */
    long countAll();
}
