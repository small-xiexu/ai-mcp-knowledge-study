package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.RagTask;
import com.xbk.knowledge.domain.repository.RagTaskRepository;
import com.xbk.knowledge.infrastructure.mapper.RagTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 任务仓储实现
 *
 * 职责：RAG 任务数据持久化访问
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class RagTaskRepositoryImpl implements RagTaskRepository {

    private final RagTaskMapper ragTaskMapper;

    @Override
    public RagTask create(RagTask task) {
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        ragTaskMapper.insertTask(task);
        return task;
    }

    @Override
    public RagTask update(RagTask task) {
        task.setUpdatedAt(LocalDateTime.now());
        ragTaskMapper.updateTask(task);
        return task;
    }

    @Override
    public RagTask findByTaskId(String taskId) {
        return ragTaskMapper.findByTaskId(taskId);
    }

    @Override
    public List<RagTask> findPage(int offset, int limit) {
        return ragTaskMapper.findPage(offset, limit);
    }

    @Override
    public long countAll() {
        return ragTaskMapper.countAll();
    }
}
