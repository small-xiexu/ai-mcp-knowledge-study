package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypePageQuery;
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

    /**
     * 分页查询任务类型
     * 统一分页口径并返回稳定的分页结构
     */
    @Override
    public PageResult<TaskType> queryTaskTypePage(TaskTypePageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询条件不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        // 查询分页数据
        List<TaskType> taskTypes = taskTypeRepository.findPage(new TaskTypePageQuery(offset, pageSize));

        // 查询总数
        long total = taskTypeRepository.countAll();

        // 计算页码
        int pageNum = (offset / pageSize) + 1;

        return PageResult.of(taskTypes, total, pageNum, pageSize);
    }

    /**
     * 查询所有任务类型
     * 为配置选择提供完整列表，避免调用方自行拼装
     */
    @Override
    public List<TaskType> queryAllTaskTypes() {
        // 使用一个较大的 pageSize 来获取所有数据
        return taskTypeRepository.findPage(new TaskTypePageQuery(0, 1000));
    }

    /**
     * 根据 ID 查询任务类型
     * 确保不存在时抛出领域异常，避免空对象传播
     */
    @Override
    public TaskType queryTaskTypeById(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("任务类型 ID 不能为空");
        }
        Long id = query.getId();
        return taskTypeRepository.findById(new IdQuery(id))
                .orElseThrow(() -> new NotFoundException("任务类型不存在，id: " + id));
    }

    /**
     * 根据任务代码查询任务类型
     * 统一任务代码查询逻辑，保持业务语义一致
     */
    @Override
    public TaskType queryTaskTypeByCode(TaskTypeCodeQuery query) {
        if (query == null || query.getTaskCode() == null) {
            throw new IllegalArgumentException("任务类型代码不能为空");
        }
        String taskCode = query.getTaskCode();
        return taskTypeRepository.findByTaskCode(new TaskTypeCodeQuery(taskCode))
                .orElseThrow(() -> new NotFoundException("任务类型不存在，code: " + taskCode));
    }

    /**
     * 创建任务类型
     * 统一校验任务类型与首选模型合法性
     */
    @Override
    public TaskType createTaskType(TaskType taskType) {
        // 检查任务类型代码是否已存在
        if (taskTypeRepository.findByTaskCode(new TaskTypeCodeQuery(taskType.getTaskCode())).isPresent()) {
            throw new IllegalArgumentException("任务类型代码已存在：" + taskType.getTaskCode());
        }

        // 检查首选模型是否存在
        if (taskType.getPreferredModelId() != null &&
                !modelConfigRepository.existsById(new IdQuery(taskType.getPreferredModelId()))) {
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

    /**
     * 更新任务类型
     * 保证唯一性与首选模型合法性，避免配置悬挂
     */
    @Override
    public TaskType updateTaskType(TaskType taskType) {
        if (taskType.getId() == null) {
            throw new IllegalArgumentException("更新操作必须提供任务类型 ID");
        }

        // 查询现有配置
        TaskType existingTaskType = taskTypeRepository.findById(new IdQuery(taskType.getId()))
                .orElseThrow(() -> new NotFoundException("任务类型不存在，id: " + taskType.getId()));

        // 检查任务类型代码是否与其他任务类型冲突
        taskTypeRepository.findByTaskCode(new TaskTypeCodeQuery(taskType.getTaskCode()))
                .ifPresent(existing -> {
                    if (!existing.getId().equals(taskType.getId())) {
                        throw new IllegalArgumentException("任务类型代码已存在：" + taskType.getTaskCode());
                    }
                });

        // 检查首选模型是否存在
        if (taskType.getPreferredModelId() != null &&
                !modelConfigRepository.existsById(new IdQuery(taskType.getPreferredModelId()))) {
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

    /**
     * 删除任务类型
     * 防止删除不存在的任务类型，保持操作语义清晰
     */
    @Override
    public void deleteTaskType(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("任务类型 ID 不能为空");
        }
        Long id = query.getId();
        // 检查任务类型是否存在
        if (!taskTypeRepository.existsById(new IdQuery(id))) {
            throw new NotFoundException("任务类型不存在，id: " + id);
        }

        // 删除任务类型
        taskTypeRepository.deleteById(new IdQuery(id));
    }
}
