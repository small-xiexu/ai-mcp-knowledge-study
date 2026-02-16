package com.xbk.knowledge.infrastructure.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;
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
     * 为什么：落库任务类型配置
     * 入参：任务类型
     * 出参：影响行数
     */
    int insertTaskType(TaskType taskType);

    /**
     * 更新任务类型
     *
     * 为什么：更新任务类型配置
     * 入参：任务类型
     * 出参：影响行数
     */
    int updateTaskType(TaskType taskType);

    /**
     * 删除任务类型
     *
     * 为什么：清理无效配置
     * 入参：ID 查询条件
     * 出参：影响行数
     */
    int deleteTaskTypeById(IdQuery query);

    /**
     * 根据ID查询任务类型
     *
     * 为什么：按唯一 ID 获取配置
     * 入参：ID 查询条件
     * 出参：任务类型
     */
    TaskType findById(IdQuery query);

    /**
     * 根据编码查询任务类型
     *
     * 为什么：按编码定位任务类型
     * 入参：任务编码查询条件
     * 出参：任务类型
     */
    TaskType findByTaskCode(TaskTypeCodeQuery query);

    /**
     * 查询任务类型分页数据
     *
     * 为什么：分页展示任务类型
     * 入参：分页查询条件
     * 出参：任务类型列表
     */
    List<TaskType> findPage(TaskTypePageQuery query);

    /**
     * 统计任务类型总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    long countAll();
}
