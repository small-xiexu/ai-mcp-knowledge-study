package com.xbk.knowledge.infrastructure.repository.rag;

import com.xbk.knowledge.domain.model.entity.RagTask;
import com.xbk.knowledge.domain.repository.rag.RagTaskRepository;
import com.xbk.knowledge.infrastructure.mapper.rag.RagTaskMapper;
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
        ragTaskMapper.insertTask(task);
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
        ragTaskMapper.updateTask(task);
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
        return ragTaskMapper.findByTaskId(taskId);
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
        return ragTaskMapper.findPage(offset, limit);
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
     * countByOrgId。
     *
     * @param orgId 参数
     * @return 返回结果
     */
    @Override
    public long countByOrgId(Long orgId) {
        if (orgId == null) {
            return 0;
        }
        return ragTaskMapper.countByOrgId(orgId);
    }

    /**
     * countByOrgIdAndStatus。
     *
     * @param orgId 参数
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public long countByOrgIdAndStatus(Long orgId, String status) {
        if (orgId == null || status == null) {
            return 0;
        }
        return ragTaskMapper.countByOrgIdAndStatus(orgId, status);
    }

    /**
     * countDistinctRagTagByOrgId。
     *
     * @param orgId 参数
     * @return 返回结果
     */
    @Override
    public long countDistinctRagTagByOrgId(Long orgId) {
        if (orgId == null) {
            return 0;
        }
        return ragTaskMapper.countDistinctRagTagByOrgId(orgId);
    }

    /**
     * countFailedTasksSince。
     *
     * @param orgId 参数
     * @param since 参数
     * @return 返回结果
     */
    @Override
    public long countFailedTasksSince(Long orgId, LocalDateTime since) {
        if (orgId == null || since == null) {
            return 0;
        }
        return ragTaskMapper.countFailedTasksSinceByOrgId(orgId, since);
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
        return ragTaskMapper.findFailedTasksSince(since);
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
        return ragTaskMapper.findProcessingTasksBefore(before);
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
