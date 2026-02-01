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
     *
     * 为什么：统一分页口径，避免前端传参与仓储不一致
     * 入参：分页查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<TaskType> queryTaskTypePage(TaskTypePageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询条件不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        /*
         * 目的：规范化分页参数，避免异常分页导致性能问题
         */
        TaskTypePageQuery pageQuery = new TaskTypePageQuery(offset, pageSize);
        List<TaskType> taskTypes = taskTypeRepository.findPage(pageQuery);

        /*
         * 目的：查询总数以支持分页组件
         */
        long total = taskTypeRepository.countAll();

        /*
         * 目的：将偏移量转换为页码以保持响应一致
         */
        int pageNum = (offset / pageSize) + 1;

        return PageResult.of(taskTypes, total, pageNum, pageSize);
    }

    /**
     * 查询所有任务类型
     *
     * 为什么：提供完整列表用于配置选择
     * 入参：无
     * 出参：任务类型列表
     */
    @Override
    public List<TaskType> queryAllTaskTypes() {
        /*
         * 约束：使用较大 pageSize 获取全量数据，适配小规模配置场景
         */
        TaskTypePageQuery pageQuery = new TaskTypePageQuery(0, 1000);
        return taskTypeRepository.findPage(pageQuery);
    }

    /**
     * 根据 ID 查询任务类型
     *
     * 为什么：不存在时抛出领域异常，避免空对象传播
     * 入参：ID 查询对象
     * 出参：任务类型
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
     *
     * 为什么：任务执行依赖代码定位类型
     * 入参：任务代码查询对象
     * 出参：任务类型
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
     *
     * 为什么：创建时保证任务代码唯一与首选模型合法
     * 入参：任务类型实体
     * 出参：创建后的任务类型
     */
    @Override
    public TaskType createTaskType(TaskType taskType) {
        /*
         * 目的：校验任务类型代码唯一性
         */
        String taskCode = taskType.getTaskCode();
        TaskTypeCodeQuery taskTypeCodeQuery = new TaskTypeCodeQuery(taskCode);
        if (taskTypeRepository
                .findByTaskCode(taskTypeCodeQuery)
                .isPresent()) {
            throw new IllegalArgumentException("任务类型代码已存在：" + taskCode);
        }

        /*
         * 目的：校验首选模型存在性，避免配置悬挂
         */
        Long preferredModelId = taskType.getPreferredModelId();
        if (preferredModelId != null) {
            IdQuery preferredModelIdQuery = new IdQuery(preferredModelId);
            if (!modelConfigRepository.existsById(preferredModelIdQuery)) {
                // 避免任务类型指向不存在的模型，保证配置可用
                throw new NotFoundException("首选模型不存在，id: " + preferredModelId);
            }
        }

        /*
         * 目的：补齐审计时间字段
         */
        LocalDateTime now = LocalDateTime.now();
        taskType.setCreatedAt(now);
        taskType.setUpdatedAt(now);

        /*
         * 目的：以聚合形式保存，保证一致性
         */
        TaskTypeAggregate aggregate = TaskTypeAggregate.builder()
                .taskType(taskType)
                .build();
        TaskTypeAggregate savedAggregate = taskTypeRepository.save(aggregate);
        return savedAggregate.getTaskType();
    }

    /**
     * 更新任务类型
     *
     * 为什么：更新前校验唯一性与首选模型合法性
     * 入参：任务类型实体
     * 出参：更新后的任务类型
     */
    @Override
    public TaskType updateTaskType(TaskType taskType) {
        if (taskType.getId() == null) {
            throw new IllegalArgumentException("更新操作必须提供任务类型 ID");
        }

        /*
         * 目的：读取现有配置，确保更新基于最新数据
         */
        Long taskTypeId = taskType.getId();
        IdQuery idQuery = new IdQuery(taskTypeId);
        String notFoundMessage = "任务类型不存在，id: " + taskTypeId;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        TaskType existingTaskType = taskTypeRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        /*
         * 目的：校验任务类型代码唯一性
         */
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

        /*
         * 目的：校验首选模型存在性，避免配置悬挂
         */
        Long preferredModelId = taskType.getPreferredModelId();
        if (preferredModelId != null) {
            IdQuery preferredModelIdQuery = new IdQuery(preferredModelId);
            if (!modelConfigRepository.existsById(preferredModelIdQuery)) {
                // 业务侧校验优先模型有效性，防止配置悬挂
                throw new NotFoundException("首选模型不存在，id: " + preferredModelId);
            }
        }

        /*
         * 目的：覆盖可更新字段并刷新更新时间
         */
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

        /*
         * 目的：以聚合形式保存，保证一致性
         */
        TaskTypeAggregate aggregate = TaskTypeAggregate.builder()
                .taskType(existingTaskType)
                .build();
        TaskTypeAggregate savedAggregate = taskTypeRepository.save(aggregate);
        return savedAggregate.getTaskType();
    }

    /**
     * 删除任务类型
     *
     * 为什么：防止删除不存在的任务类型，保持操作语义清晰
     * 入参：ID 查询对象
     * 出参：无
     */
    @Override
    public void deleteTaskType(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("任务类型 ID 不能为空");
        }
        Long id = query.getId();
        /*
         * 目的：先检查存在性，避免静默失败
         */
        IdQuery idQuery = new IdQuery(id);
        if (!taskTypeRepository.existsById(idQuery)) {
            throw new NotFoundException("任务类型不存在，id: " + id);
        }

        /*
         * 目的：执行删除，释放配置
         */
        taskTypeRepository.deleteById(idQuery);
    }
}
