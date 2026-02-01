package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.TaskTypeAppService;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;
import com.xbk.knowledge.domain.service.ITaskTypeService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务类型应用服务实现
 * 负责任务类型相关用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class TaskTypeAppServiceImpl implements TaskTypeAppService {

    private final ITaskTypeService taskTypeService;

    /**
     * 分页查询任务类型
     *
     * 为什么：统一分页入口，隔离应用层与领域层协议
     * 入参：分页查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<TaskType> queryTaskTypePage(TaskTypePageQuery query) {
        return taskTypeService.queryTaskTypePage(query);
    }

    /**
     * 查询所有任务类型
     *
     * 为什么：提供全量列表供下拉或配置使用
     * 入参：无
     * 出参：任务类型列表
     */
    @Override
    public List<TaskType> queryAllTaskTypes() {
        return taskTypeService.queryAllTaskTypes();
    }

    /**
     * 根据 ID 查询任务类型
     *
     * 为什么：统一详情查询入口，便于扩展校验
     * 入参：ID 查询对象
     * 出参：任务类型详情
     */
    @Override
    public TaskType queryTaskTypeById(IdQuery query) {
        return taskTypeService.queryTaskTypeById(query);
    }

    /**
     * 根据任务代码查询任务类型
     *
     * 为什么：任务执行依赖代码定位类型
     * 入参：任务代码查询对象
     * 出参：任务类型详情
     */
    @Override
    public TaskType queryTaskTypeByCode(TaskTypeCodeQuery query) {
        return taskTypeService.queryTaskTypeByCode(query);
    }

    /**
     * 创建任务类型
     *
     * 为什么：由应用层控制事务边界，保证创建一致性
     * 入参：任务类型实体
     * 出参：创建后的任务类型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskType createTaskType(TaskType taskType) {
        return taskTypeService.createTaskType(taskType);
    }

    /**
     * 更新任务类型
     *
     * 为什么：由应用层控制事务边界，保证更新一致性
     * 入参：任务类型实体
     * 出参：更新后的任务类型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskType updateTaskType(TaskType taskType) {
        return taskTypeService.updateTaskType(taskType);
    }

    /**
     * 删除任务类型
     *
     * 为什么：由应用层控制事务边界，保证删除一致性
     * 入参：ID 查询对象
     * 出参：无
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaskType(IdQuery query) {
        taskTypeService.deleteTaskType(query);
    }
}
