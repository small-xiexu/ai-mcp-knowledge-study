package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.aggregate.task.TaskTypeAggregate;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        TaskTypePageQuery pageQuery = new TaskTypePageQuery(offset, pageSize);
        List<TaskType> taskTypes = taskTypeRepository.findPage(pageQuery);

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
        TaskTypePageQuery pageQuery = new TaskTypePageQuery(0, 1000);
        return taskTypeRepository.findPage(pageQuery);
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
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "任务类型不存在，id: " + id;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        return taskTypeRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);
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
        TaskTypeCodeQuery taskTypeCodeQuery = new TaskTypeCodeQuery(taskCode);
        String notFoundMessage = "任务类型不存在，code: " + taskCode;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        return taskTypeRepository
                .findByTaskCode(taskTypeCodeQuery)
                .orElseThrow(exceptionSupplier);
    }

    /**
     * 创建任务类型
     * 统一校验任务类型与首选模型合法性
     */
    @Override
    public TaskType createTaskType(TaskType taskType) {
        // 检查任务类型代码是否已存在
        String taskCode = taskType.getTaskCode();
        TaskTypeCodeQuery taskTypeCodeQuery = new TaskTypeCodeQuery(taskCode);
        if (taskTypeRepository
                .findByTaskCode(taskTypeCodeQuery)
                .isPresent()) {
            throw new IllegalArgumentException("任务类型代码已存在：" + taskCode);
        }

        // 检查首选模型是否存在
        Long preferredModelId = taskType.getPreferredModelId();
        if (preferredModelId != null) {
            IdQuery preferredModelIdQuery = new IdQuery(preferredModelId);
            if (!modelConfigRepository.existsById(preferredModelIdQuery)) {
                // 避免任务类型指向不存在的模型，保证配置可用
                throw new NotFoundException("首选模型不存在，id: " + preferredModelId);
            }
        }

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        taskType.setCreatedAt(now);
        taskType.setUpdatedAt(now);

        // 保存到数据库
        TaskTypeAggregate aggregate = TaskTypeAggregate.builder()
                .taskType(taskType)
                .build();
        TaskTypeAggregate savedAggregate = taskTypeRepository.save(aggregate);
        return savedAggregate.getTaskType();
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
        Long taskTypeId = taskType.getId();
        IdQuery idQuery = new IdQuery(taskTypeId);
        String notFoundMessage = "任务类型不存在，id: " + taskTypeId;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        TaskType existingTaskType = taskTypeRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        // 检查任务类型代码是否与其他任务类型冲突
        String taskCode = taskType.getTaskCode();
        TaskTypeCodeQuery taskTypeCodeQuery = new TaskTypeCodeQuery(taskCode);
        Consumer<TaskType> duplicateChecker = existing -> {
            if (!existing
                    .getId()
                    .equals(taskType
                    .getId())) {
                throw new IllegalArgumentException("任务类型代码已存在：" + taskCode);
            }
        };
        taskTypeRepository
                .findByTaskCode(taskTypeCodeQuery)
                .ifPresent(duplicateChecker);

        // 检查首选模型是否存在
        Long preferredModelId = taskType.getPreferredModelId();
        if (preferredModelId != null) {
            IdQuery preferredModelIdQuery = new IdQuery(preferredModelId);
            if (!modelConfigRepository.existsById(preferredModelIdQuery)) {
                // 业务侧校验优先模型有效性，防止配置悬挂
                throw new NotFoundException("首选模型不存在，id: " + preferredModelId);
            }
        }

        // 更新字段
        String taskName = taskType.getTaskName();
        String description = taskType.getDescription();
        String fallbackModelIds = taskType.getFallbackModelIds();
        LocalDateTime updatedAt = LocalDateTime.now();
        existingTaskType.setTaskCode(taskCode);
        existingTaskType.setTaskName(taskName);
        existingTaskType.setDescription(description);
        existingTaskType.setPreferredModelId(preferredModelId);
        existingTaskType.setFallbackModelIds(fallbackModelIds);
        existingTaskType.setUpdatedAt(updatedAt);

        // 保存更新
        TaskTypeAggregate aggregate = TaskTypeAggregate.builder()
                .taskType(existingTaskType)
                .build();
        TaskTypeAggregate savedAggregate = taskTypeRepository.save(aggregate);
        return savedAggregate.getTaskType();
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
        IdQuery idQuery = new IdQuery(id);
        if (!taskTypeRepository.existsById(idQuery)) {
            throw new NotFoundException("任务类型不存在，id: " + id);
        }

        // 删除任务类型
        taskTypeRepository.deleteById(idQuery);
    }
}
