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
 * WorkflowRunRepositoryImpl。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRunRepositoryImpl implements WorkflowRunRepository {

    private final IWorkflowRunDao mapper;

    /**
     * insert。
     *
     * @param run 参数
     */
    @Override
    public void insert(WorkflowRun run) {
        if (run == null) {
            return;
        }
        mapper.insertRun(BeanMappingUtils.map(run, WorkflowRunPO.class));
    }

    /**
     * updateStatus。
     *
     * @param runId 参数
     * @param status 参数
     * @param errorMessage 参数
     * @param endedAt 参数
     * @return 返回结果
     */
    @Override
    public int updateStatus(String runId, String status, String errorMessage, LocalDateTime endedAt) {
        if (runId == null || status == null) {
            return 0;
        }
        return mapper.updateStatus(runId, status, errorMessage, endedAt);
    }

    /**
     * updateStatusAndMetrics。
     *
     * @param run 参数
     */
    @Override
    public void updateStatusAndMetrics(WorkflowRun run) {
        if (run == null || run.getRunId() == null) {
            return;
        }
        mapper.updateStatusAndMetrics(BeanMappingUtils.map(run, WorkflowRunPO.class));
    }

    /**
     * findByRunId。
     *
     * @param runId 参数
     * @return 返回结果
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
     * list。
     *
     * @param status 参数
     * @param offset 参数
     * @param pageSize 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowRun> list(String status, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return BeanMappingUtils.mapList(mapper.list(status, safeOffset, safeSize), WorkflowRun.class);
    }

    /**
     * count。
     *
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public long count(String status) {
        return mapper.count(status);
    }

    /**
     * deleteBefore。
     *
     * @param cutOff 参数
     * @param limit 参数
     * @return 返回结果
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
     * listRunIdsBefore。
     *
     * @param cutOff 参数
     * @param limit 参数
     * @return 返回结果
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
     * deleteByRunIds。
     *
     * @param runIds 参数
     * @return 返回结果
     */
    @Override
    public int deleteByRunIds(List<String> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return 0;
        }
        return mapper.deleteByRunIds(runIds);
    }
}
