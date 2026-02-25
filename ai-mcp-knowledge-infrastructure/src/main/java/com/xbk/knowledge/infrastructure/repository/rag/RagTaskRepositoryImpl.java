package com.xbk.knowledge.infrastructure.repository.rag;

import com.xbk.knowledge.domain.rag.model.entity.RagTask;
import com.xbk.knowledge.domain.rag.adapter.repository.RagTaskRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IRagTaskDao;
import com.xbk.knowledge.infrastructure.dao.po.RagTaskPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 任务仓储实现
 *
 * 职责：RAG 任务数据持久化访问
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class RagTaskRepositoryImpl implements RagTaskRepository {

    /**
     * RAG 任务数据访问对象。
     */
    private final IRagTaskDao ragTaskMapper;

    /**
     * 新建任务
     *
     * 落库时补齐时间戳，保证审计字段一致
     * 
     * @param task 待创建的任务实体。
     * @return 已持久化的任务实体。
     */
    @Override
    public RagTask create(RagTask task) {
        // 基础设施层统一维护时间戳，避免上层重复设置
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        ragTaskMapper.insertTask(BeanMappingUtils.map(task, RagTaskPO.class));
        return task;
    }

    /**
     * 更新任务
     *
     * 更新时刷新更新时间，保持审计一致
     * 
     * @param task 待更新的任务实体。
     * @return 更新后的任务实体。
     */
    @Override
    public RagTask update(RagTask task) {
        task.setUpdatedAt(LocalDateTime.now());
        ragTaskMapper.updateTask(BeanMappingUtils.map(task, RagTaskPO.class));
        return task;
    }

    /**
     * 按任务 ID 查询
     *
     * 获取任务当前状态
     * 
     * @param taskId 任务 ID。
     * @return 任务实体。
     */
    @Override
    public RagTask findByTaskId(String taskId) {
        return BeanMappingUtils.map(ragTaskMapper.findByTaskId(taskId), RagTask.class);
    }

    /**
     * 分页查询任务
     *
     * 控制单次返回数量
     * 
     * @param offset 分页偏移量。
     * @param limit 分页大小。
     * @return 任务列表。
     */
    @Override
    public List<RagTask> findPage(int offset, int limit) {
        return BeanMappingUtils.mapList(ragTaskMapper.findPage(offset, limit), RagTask.class);
    }

    /**
     * 统计任务总数
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    @Override
    public long countAll() {
        return ragTaskMapper.countAll();
    }

    /**
     * 按条件统计业务数量。
     * 
     * @param status 状态值
     * @return 统计数量
     */
    @Override
    public long countByStatus(String status) {
        if (status == null) {
            return 0;
        }
        return ragTaskMapper.countByStatus(status);
    }

    /**
     * 按条件统计业务数量。
     * 
     * @return 统计数量
     */
    @Override
    public long countDistinctRagTag() {
        return ragTaskMapper.countDistinctRagTag();
    }

    /**
     * 按条件统计业务数量。
     * 
     * @param since 起始时间。
     * @return 统计数量
     */
    @Override
    public long countFailedTasksSince(LocalDateTime since) {
        if (since == null) {
            return 0;
        }
        return ragTaskMapper.countFailedTasksSince(since);
    }

    /**
     * 查询指定时间后失败的任务
     *
     * 支持失败重试或告警统计
     * 
     * @param since 起始时间（查询该时间之后失败的任务）。
     * @return 失败任务列表。
     */
    @Override
    public List<RagTask> findFailedTasksSince(LocalDateTime since) {
        return BeanMappingUtils.mapList(ragTaskMapper.findFailedTasksSince(since), RagTask.class);
    }

    /**
     * 查询指定时间前仍处于处理中的任务
     *
     * 识别超时任务用于清理或重试
     * 
     * @param before 截止时间（早于该时间且仍处理中）。
     * @return 超时候选任务列表。
     */
    @Override
    public List<RagTask> findProcessingTasksBefore(LocalDateTime before) {
        return BeanMappingUtils.mapList(ragTaskMapper.findProcessingTasksBefore(before), RagTask.class);
    }

    /**
     * 删除指定时间前已完成的任务
     *
     * 定期清理历史任务
     * 
     * @param before 截止时间（早于该时间的已完成任务会被删除）。
     * @return 影响行数。
     */
    @Override
    public int deleteCompletedTasksBefore(LocalDateTime before) {
        return ragTaskMapper.deleteCompletedTasksBefore(before);
    }
}
