package com.xbk.knowledge.application.service.impl;

import com.xbk.knowledge.application.service.TaskTypeAppService;
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
     * 负责应用层用例编排，调用领域服务获取分页结果
     */
    @Override
    public PageResult<TaskType> queryTaskTypePage(TaskTypePageQuery query) {
        return taskTypeService.queryTaskTypePage(query);
    }

    /**
     * 查询所有任务类型
     * 负责应用层用例编排，调用领域服务返回任务类型列表
     */
    @Override
    public List<TaskType> queryAllTaskTypes() {
        return taskTypeService.queryAllTaskTypes();
    }

    /**
     * 根据 ID 查询任务类型
     * 负责应用层用例编排，调用领域服务获取任务类型详情
     */
    @Override
    public TaskType queryTaskTypeById(IdQuery query) {
        return taskTypeService.queryTaskTypeById(query);
    }

    /**
     * 根据任务代码查询任务类型
     * 负责应用层用例编排，调用领域服务获取任务类型信息
     */
    @Override
    public TaskType queryTaskTypeByCode(TaskTypeCodeQuery query) {
        return taskTypeService.queryTaskTypeByCode(query);
    }

    /**
     * 创建任务类型
     * 负责应用层事务边界编排，确保创建操作一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskType createTaskType(TaskType taskType) {
        return taskTypeService.createTaskType(taskType);
    }

    /**
     * 更新任务类型
     * 负责应用层事务边界编排，确保更新操作一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskType updateTaskType(TaskType taskType) {
        return taskTypeService.updateTaskType(taskType);
    }

    /**
     * 删除任务类型
     * 负责应用层事务边界编排，确保删除操作一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaskType(IdQuery query) {
        taskTypeService.deleteTaskType(query);
    }
}
