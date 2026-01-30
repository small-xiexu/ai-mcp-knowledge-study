package com.xbk.knowledge.domain.service;

import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * 任务类型领域服务接口
 * 负责任务类型的业务逻辑处理
 *
 * 职责：领域服务接口，用于定义业务能力
 * @author xiexu
 */
public interface ITaskTypeService {

    /**
     * 分页查询任务类型
     *
     * @param query 分页查询条件
     * @return 分页结果
     */
    PageResult<TaskType> queryTaskTypePage(TaskTypePageQuery query);

    /**
     * 查询所有任务类型
     *
     * @return 任务类型列表
     */
    List<TaskType> queryAllTaskTypes();

    /**
     * 根据 ID 查询任务类型
     *
     * @param query ID 查询条件
     * @return 任务类型
     */
    TaskType queryTaskTypeById(IdQuery query);

    /**
     * 根据任务代码查询任务类型
     *
     * @param query 任务代码查询条件
     * @return 任务类型
     */
    TaskType queryTaskTypeByCode(TaskTypeCodeQuery query);

    /**
     * 创建任务类型
     *
     * @param taskType 任务类型实体
     * @return 创建后的任务类型
     */
    TaskType createTaskType(TaskType taskType);

    /**
     * 更新任务类型
     *
     * @param taskType 任务类型实体（必须包含 ID）
     * @return 更新后的任务类型
     */
    TaskType updateTaskType(TaskType taskType);

    /**
     * 删除任务类型
     *
     * @param query ID 查询条件
     */
    void deleteTaskType(IdQuery query);
}
