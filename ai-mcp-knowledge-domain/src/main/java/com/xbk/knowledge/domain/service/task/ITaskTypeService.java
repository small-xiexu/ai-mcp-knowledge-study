package com.xbk.knowledge.domain.service.task;

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
     * 为什么：统一分页查询能力入口
     * 入参：分页查询条件
     * 出参：分页结果
     */
    PageResult<TaskType> queryTaskTypePage(TaskTypePageQuery query);

    /**
     * 查询所有任务类型
     *
     * 为什么：提供下拉或配置列表数据源
     * 入参：无
     * 出参：任务类型列表
     */
    List<TaskType> queryAllTaskTypes();

    /**
     * 根据 ID 查询任务类型
     *
     * 为什么：按唯一 ID 获取任务类型
     * 入参：ID 查询条件
     * 出参：任务类型
     */
    TaskType queryTaskTypeById(IdQuery query);

    /**
     * 根据任务代码查询任务类型
     *
     * 为什么：任务执行依赖代码定位类型
     * 入参：任务代码查询条件
     * 出参：任务类型
     */
    TaskType queryTaskTypeByCode(TaskTypeCodeQuery query);

    /**
     * 创建任务类型
     *
     * 为什么：统一创建入口以保障规则一致
     * 入参：任务类型实体
     * 出参：创建后的任务类型
     */
    TaskType createTaskType(TaskType taskType);

    /**
     * 更新任务类型
     *
     * 为什么：统一更新入口以保障规则一致
     * 入参：任务类型实体（必须包含 ID）
     * 出参：更新后的任务类型
     */
    TaskType updateTaskType(TaskType taskType);

    /**
     * 删除任务类型
     *
     * 为什么：统一删除入口以保障规则一致
     * 入参：ID 查询条件
     * 出参：无
     */
    void deleteTaskType(IdQuery query);
}
