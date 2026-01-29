package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.repository.TaskTypeRepository;
import com.xbk.knowledge.domain.service.ITaskTypeService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务类型领域服务实现
 * 封装任务类型的业务逻辑
 *
 * 职责：领域服务实现，用于封装业务规则
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskTypeServiceImpl implements ITaskTypeService {

    private final TaskTypeRepository taskTypeRepository;
    private final ModelConfigRepository modelConfigRepository;

    @Override
    public PageResult<TaskType> queryTaskTypePage(int offset, int pageSize) {
        // 查询分页数据
        List<TaskType> taskTypes = taskTypeRepository.findPage(offset, pageSize);

        // 查询总数
        long total = taskTypeRepository.countAll();

        // 计算页码
        int pageNum = (offset / pageSize) + 1;

        return PageResult.of(taskTypes, total, pageNum, pageSize);
    }

    @Override
    public List<TaskType> queryAllTaskTypes() {
        // 使用一个较大的 pageSize 来获取所有数据
        return taskTypeRepository.findPage(0, 1000);
    }

    @Override
    public TaskType queryTaskTypeById(Long id) {
        return taskTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("任务类型不存在，id: " + id));
    }

    @Override
    public TaskType queryTaskTypeByCode(String code) {
        return taskTypeRepository.findByTaskCode(code)
                .orElseThrow(() -> new NotFoundException("任务类型不存在，code: " + code));
    }

    @Override
    public TaskType createTaskType(TaskType taskType) {
        // 检查任务类型代码是否已存在
        if (taskTypeRepository.findByTaskCode(taskType.getTaskCode()).isPresent()) {
            throw new IllegalArgumentException("任务类型代码已存在：" + taskType.getTaskCode());
        }

        // 检查首选模型是否存在
        if (taskType.getPreferredModelId() != null &&
                !modelConfigRepository.existsById(taskType.getPreferredModelId())) {
            // 避免任务类型指向不存在的模型，保证配置可用
            throw new NotFoundException("首选模型不存在，id: " + taskType.getPreferredModelId());
        }

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        taskType.setCreatedAt(now);
        taskType.setUpdatedAt(now);

        // 保存到数据库
        return taskTypeRepository.save(taskType);
    }

    @Override
    public TaskType updateTaskType(TaskType taskType) {
        if (taskType.getId() == null) {
            throw new IllegalArgumentException("更新操作必须提供任务类型 ID");
        }

        // 查询现有配置
        TaskType existingTaskType = taskTypeRepository.findById(taskType.getId())
                .orElseThrow(() -> new NotFoundException("任务类型不存在，id: " + taskType.getId()));

        // 检查任务类型代码是否与其他任务类型冲突
        taskTypeRepository.findByTaskCode(taskType.getTaskCode())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(taskType.getId())) {
                        throw new IllegalArgumentException("任务类型代码已存在：" + taskType.getTaskCode());
                    }
                });

        // 检查首选模型是否存在
        if (taskType.getPreferredModelId() != null &&
                !modelConfigRepository.existsById(taskType.getPreferredModelId())) {
            // 业务侧校验优先模型有效性，防止配置悬挂
            throw new NotFoundException("首选模型不存在，id: " + taskType.getPreferredModelId());
        }

        // 更新字段
        existingTaskType.setTaskCode(taskType.getTaskCode());
        existingTaskType.setTaskName(taskType.getTaskName());
        existingTaskType.setDescription(taskType.getDescription());
        existingTaskType.setPreferredModelId(taskType.getPreferredModelId());
        existingTaskType.setFallbackModelIds(taskType.getFallbackModelIds());
        existingTaskType.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        return taskTypeRepository.save(existingTaskType);
    }

    @Override
    public void deleteTaskType(Long id) {
        // 检查任务类型是否存在
        if (!taskTypeRepository.existsById(id)) {
            throw new NotFoundException("任务类型不存在，id: " + id);
        }

        // 删除任务类型
        taskTypeRepository.deleteById(id);
    }
}
