package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.aggregate.task.TaskTypeAggregate;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;
import com.xbk.knowledge.domain.repository.TaskTypeRepository;
import com.xbk.knowledge.infrastructure.mapper.TaskTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 任务类型仓储实现
 * 通过 Mapper 执行 XML SQL，隔离持久化细节
 *
 * 职责：仓储实现，用于落地数据访问
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class TaskTypeRepositoryImpl implements TaskTypeRepository {

    private final TaskTypeMapper taskTypeMapper;

    /**
     * 按任务编码查询
     * 用于唯一性校验与业务定位
     */
    @Override
    public Optional<TaskType> findByTaskCode(TaskTypeCodeQuery query) {
        if (query == null || query.getTaskCode() == null) {
            return Optional.empty();
        }
        TaskType taskType = taskTypeMapper.findByTaskCode(query);
        return Optional.ofNullable(taskType);
    }

    /**
     * 按 ID 查询任务类型
     * 用于详情展示与编辑加载
     */
    @Override
    public Optional<TaskType> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        TaskType taskType = taskTypeMapper.findById(query);
        return Optional.ofNullable(taskType);
    }

    /**
     * 保存任务类型
     * 统一插入与更新逻辑并补齐时间戳
     */
    @Override
    public TaskTypeAggregate save(TaskTypeAggregate aggregate) {
        if (aggregate == null || aggregate.getTaskType() == null) {
            return aggregate;
        }
        TaskType taskType = aggregate.getTaskType();
        LocalDateTime now = LocalDateTime.now();
        if (taskType.getId() == null) {
            if (taskType.getCreatedAt() == null) {
                taskType.setCreatedAt(now);
            }
            if (taskType.getUpdatedAt() == null) {
                taskType.setUpdatedAt(now);
            }
            taskTypeMapper.insertTaskType(taskType);
            aggregate.setTaskType(taskType);
            return aggregate;
        }
        if (taskType.getUpdatedAt() == null) {
            taskType.setUpdatedAt(now);
        }
        taskTypeMapper.updateTaskType(taskType);
        aggregate.setTaskType(taskType);
        return aggregate;
    }

    /**
     * 判断任务类型是否存在
     * 用于删除与更新前置校验
     */
    @Override
    public boolean existsById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return false;
        }
        return taskTypeMapper.findById(query) != null;
    }

    /**
     * 删除任务类型
     * 允许空 ID 直接返回，避免无效调用
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        taskTypeMapper.deleteTaskTypeById(query);
    }

    /**
     * 分页查询任务类型
     * 用于配置管理列表展示
     */
    @Override
    public List<TaskType> findPage(TaskTypePageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return taskTypeMapper.findPage(query);
    }

    /**
     * 统计任务类型总数
     * 用于分页统计
     */
    @Override
    public long countAll() {
        return taskTypeMapper.countAll();
    }
}
