package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.repository.TaskTypeRepository;
import com.xbk.knowledge.infrastructure.mapper.TaskTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Override
    public Optional<TaskType> findByTaskCode(String taskCode) {
        return Optional.ofNullable(taskTypeMapper.findByTaskCode(taskCode));
    }

    @Override
    public Optional<TaskType> findById(Long id) {
        return Optional.ofNullable(taskTypeMapper.findById(id));
    }

    @Override
    public TaskType save(TaskType taskType) {
        LocalDateTime now = LocalDateTime.now();
        if (taskType.getId() == null) {
            if (taskType.getCreatedAt() == null) {
                taskType.setCreatedAt(now);
            }
            if (taskType.getUpdatedAt() == null) {
                taskType.setUpdatedAt(now);
            }
            taskTypeMapper.insertTaskType(taskType);
            return taskType;
        }
        if (taskType.getUpdatedAt() == null) {
            taskType.setUpdatedAt(now);
        }
        taskTypeMapper.updateTaskType(taskType);
        return taskType;
    }

    @Override
    public boolean existsById(Long id) {
        return taskTypeMapper.findById(id) != null;
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        taskTypeMapper.deleteTaskTypeById(id);
    }

    @Override
    public List<TaskType> findPage(int offset, int pageSize) {
        return taskTypeMapper.findPage(offset, pageSize);
    }

    @Override
    public long countAll() {
        return taskTypeMapper.countAll();
    }
}
