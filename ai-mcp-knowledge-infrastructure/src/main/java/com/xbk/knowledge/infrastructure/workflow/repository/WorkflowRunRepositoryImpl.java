package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRun;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IWorkflowRunDao;
import com.xbk.knowledge.infrastructure.dao.po.WorkflowRunPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 创建并持久化工作流运行数据。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRunRepositoryImpl implements WorkflowRunRepository {

    /**
     * Workflow 运行记录数据访问对象。
     */
    private final IWorkflowRunDao mapper;

    /**
     * 创建并持久化工作流运行数据。
     *
     * @param run 运行记录
     */
    @Override
    public void insert(WorkflowRun run) {
        if (run == null) {
            return;
        }
        mapper.insertRun(BeanMappingUtils.map(run, WorkflowRunPO.class));
    }

    /**
     * 更新工作流运行数据。
     *
     * @param runId 运行 ID
     * @param status 状态值
     * @param errorMessage 错误信息
     * @param endedAt 结束时间
     * @return 运行状态更新条数
     */
    @Override
    public int updateStatus(String runId, String status, String errorMessage, LocalDateTime endedAt) {
        if (runId == null || status == null) {
            return 0;
        }
        return mapper.updateStatus(runId, status, errorMessage, endedAt);
    }

    /**
     * 更新工作流运行数据。
     *
     * @param run 运行记录
     */
    @Override
    public void updateStatusAndMetrics(WorkflowRun run) {
        if (run == null || run.getRunId() == null) {
            return;
        }
        mapper.updateStatusAndMetrics(BeanMappingUtils.map(run, WorkflowRunPO.class));
    }

    /**
     * 查询工作流运行。
     *
     * @param runId 运行 ID
     * @return WorkflowRun 查询结果（可能为空）
     */
    @Override
    public Optional<WorkflowRun> findByRunId(String runId) {
        if (runId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByRunId(runId))
                .map(item -> BeanMappingUtils.map(item, WorkflowRun.class));
    }

    /**
     * 根据筛选条件查询工作流运行列表。
     *
     * @param status 状态值
     * @param offset 分页偏移量
     * @param pageSize 分页大小
     * @return WorkflowRun 列表
     */
    @Override
    public List<WorkflowRun> list(String status, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return BeanMappingUtils.mapList(mapper.list(status, safeOffset, safeSize), WorkflowRun.class);
    }

    /**
     * 按条件统计业务数量。
     *
     * @param status 状态值
     * @return 统计数量
     */
    @Override
    public long count(String status) {
        return mapper.count(status);
    }

    /**
     * 删除工作流运行数据。
     *
     * @param cutOff 截止时间
     * @param limit 限制数量
     * @return 运行记录删除条数
     */
    @Override
    public int deleteBefore(LocalDateTime cutOff, int limit) {
        if (cutOff == null) {
            return 0;
        }
        int safeLimit = limit <= 0 ? 2000 : Math.min(limit, 10000);
        return mapper.deleteBefore(cutOff, safeLimit);
    }

    /**
     * 根据筛选条件查询工作流运行列表。
     *
     * @param cutOff 截止时间
     * @param limit 限制数量
     * @return 运行 ID 列表
     */
    @Override
    public List<String> listRunIdsBefore(LocalDateTime cutOff, int limit) {
        if (cutOff == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit <= 0 ? 2000 : Math.min(limit, 10000);
        List<String> ids = mapper.listRunIdsBefore(cutOff, safeLimit);
        return ids == null ? Collections.emptyList() : ids;
    }

    /**
     * 删除工作流运行数据。
     *
     * @param runIds 运行 ID 列表
     * @return 运行记录删除条数
     */
    @Override
    public int deleteByRunIds(List<String> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return 0;
        }
        return mapper.deleteByRunIds(runIds);
    }
}
