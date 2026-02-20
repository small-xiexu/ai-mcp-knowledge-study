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

    private final IRagTaskDao ragTaskMapper;

    /**
     * 新建任务
     *
     * 为什么：落库时补齐时间戳，保证审计字段一致
     * 入参：任务实体
     * 出参：持久化后的任务
     */
    @Override
    public RagTask create(RagTask task) {
        /*
         * 目的：基础设施层统一维护时间戳，避免上层重复设置
 */
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        ragTaskMapper.insertTask(BeanMappingUtils.map(task, RagTaskPO.class));
        return task;
    }

    /**
     * 更新任务
     *
     * 为什么：更新时刷新更新时间，保持审计一致
     * 入参：任务实体
     * 出参：更新后的任务
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
     * 为什么：获取任务当前状态
     * 入参：任务 ID
     * 出参：任务实体
     */
    @Override
    public RagTask findByTaskId(String taskId) {
        return BeanMappingUtils.map(ragTaskMapper.findByTaskId(taskId), RagTask.class);
    }

    /**
     * 分页查询任务
     *
     * 为什么：控制单次返回数量
     * 入参：偏移量、条数
     * 出参：任务列表
     */
    @Override
    public List<RagTask> findPage(int offset, int limit) {
        return BeanMappingUtils.mapList(ragTaskMapper.findPage(offset, limit), RagTask.class);
    }

    /**
     * 统计任务总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    @Override
    public long countAll() {
        return ragTaskMapper.countAll();
    }

    /**
     * countByStatus。
     *
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public long countByStatus(String status) {
        if (status == null) {
            return 0;
        }
        return ragTaskMapper.countByStatus(status);
    }

    /**
     * countDistinctRagTag。
     *
     * @return 返回结果
     */
    @Override
    public long countDistinctRagTag() {
        return ragTaskMapper.countDistinctRagTag();
    }

    /**
     * countFailedTasksSince。
     *
     * @param since 参数
     * @return 返回结果
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
     * 为什么：支持失败重试或告警统计
     * 入参：起始时间
     * 出参：失败任务列表
     */
    @Override
    public List<RagTask> findFailedTasksSince(LocalDateTime since) {
        return BeanMappingUtils.mapList(ragTaskMapper.findFailedTasksSince(since), RagTask.class);
    }

    /**
     * 查询指定时间前仍处于处理中的任务
     *
     * 为什么：识别超时任务用于清理或重试
     * 入参：截止时间
     * 出参：任务列表
     */
    @Override
    public List<RagTask> findProcessingTasksBefore(LocalDateTime before) {
        return BeanMappingUtils.mapList(ragTaskMapper.findProcessingTasksBefore(before), RagTask.class);
    }

    /**
     * 删除指定时间前已完成的任务
     *
     * 为什么：定期清理历史任务
     * 入参：截止时间
     * 出参：删除数量
     */
    @Override
    public int deleteCompletedTasksBefore(LocalDateTime before) {
        return ragTaskMapper.deleteCompletedTasksBefore(before);
    }
}
