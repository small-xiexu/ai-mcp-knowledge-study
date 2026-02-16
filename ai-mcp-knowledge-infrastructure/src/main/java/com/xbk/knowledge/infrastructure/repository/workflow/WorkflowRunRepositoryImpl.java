package com.xbk.knowledge.infrastructure.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRun;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRunRepository;
import com.xbk.knowledge.infrastructure.mapper.workflow.WorkflowRunMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowRunRepositoryImpl。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRunRepositoryImpl implements WorkflowRunRepository {

    private final WorkflowRunMapper mapper;

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

    /**
     * insert。
     *
     * @param run 参数
     * @return 返回结果
     */
    @Override
    public WorkflowRun insert(WorkflowRun run) {
        if (run == null) {
            return null;
        }
        mapper.insertRun(run);
        return run;
    }

    /**
     * updateStatus。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param status 参数
     * @param errorMessage 参数
     * @param endedAt 参数
     * @return 返回结果
     */
    @Override
    public int updateStatus(Long orgId, String runId, String status, String errorMessage, LocalDateTime endedAt) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (runId == null || status == null) {
            return 0;
        }
        return mapper.updateStatus(orgId, runId, status, errorMessage, endedAt);
    }

    /**
     * updateStatusAndMetrics。
     *
     * @param run 参数
     * @return 返回结果
     */
    @Override
    public int updateStatusAndMetrics(WorkflowRun run) {
        if (run == null || run.getOrgId() == null || run.getRunId() == null) {
            return 0;
        }
        return mapper.updateStatusAndMetrics(run);
    }

    /**
     * findByRunId。
     *
     * @param orgId 参数
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public Optional<WorkflowRun> findByRunId(Long orgId, String runId) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (runId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByRunId(orgId, runId));
    }

    /**
     * list。
     *
     * @param orgId 参数
     * @param status 参数
     * @param offset 参数
     * @param pageSize 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowRun> list(Long orgId, String status, int offset, int pageSize) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return mapper.list(orgId, status, safeOffset, safeSize);
    }

    /**
     * count。
     *
     * @param orgId 参数
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public long count(Long orgId, String status) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        return mapper.count(orgId, status);
    }

    /**
     * deleteBefore。
     *
     * @param orgId 参数
     * @param cutOff 参数
     * @param limit 参数
     * @return 返回结果
     */
    @Override
    public int deleteBefore(Long orgId, LocalDateTime cutOff, int limit) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (cutOff == null) {
            return 0;
        }
        int safeLimit = limit <= 0 ? 2000 : Math.min(limit, 10000);
        return mapper.deleteBefore(orgId, cutOff, safeLimit);
    }

    /**
     * listRunIdsBefore。
     *
     * @param orgId 参数
     * @param cutOff 参数
     * @param limit 参数
     * @return 返回结果
     */
    @Override
    public List<String> listRunIdsBefore(Long orgId, LocalDateTime cutOff, int limit) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (cutOff == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit <= 0 ? 2000 : Math.min(limit, 10000);
        List<String> ids = mapper.listRunIdsBefore(orgId, cutOff, safeLimit);
        return ids == null ? Collections.emptyList() : ids;
    }

    /**
     * deleteByRunIds。
     *
     * @param orgId 参数
     * @param runIds 参数
     * @return 返回结果
     */
    @Override
    public int deleteByRunIds(Long orgId, List<String> runIds) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (runIds == null || runIds.isEmpty()) {
            return 0;
        }
        return mapper.deleteByRunIds(orgId, runIds);
    }
}
