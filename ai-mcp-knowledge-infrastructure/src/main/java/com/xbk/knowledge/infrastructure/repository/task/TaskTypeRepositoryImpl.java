package com.xbk.knowledge.infrastructure.repository.task;

import com.xbk.knowledge.domain.model.aggregate.task.TaskTypeAggregate;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;
import com.xbk.knowledge.domain.repository.task.TaskTypeRepository;
import com.xbk.knowledge.infrastructure.mapper.task.TaskTypeMapper;
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
     *
     * 为什么：编码用于唯一性校验
     * 入参：任务编码查询条件
     * 出参：任务类型
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
     *
     * 为什么：按唯一 ID 定位任务类型
     * 入参：ID 查询条件
     * 出参：任务类型
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
     *
     * 为什么：统一新增与更新入口
     * 入参：任务类型聚合
     * 出参：保存后的聚合
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
     *
     * 为什么：避免更新/删除不存在的数据
     * 入参：ID 查询条件
     * 出参：是否存在
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
     *
     * 为什么：清理无效配置
     * 入参：ID 查询条件
     * 出参：无
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
     *
     * 为什么：控制单次返回数量
     * 入参：分页查询条件
     * 出参：任务类型列表
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
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    @Override
    public long countAll() {
        return taskTypeMapper.countAll();
    }
}
