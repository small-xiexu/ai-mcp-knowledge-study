package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRunContext;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunContextRepository;
import com.xbk.knowledge.infrastructure.dao.IWorkflowRunContextDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.List;

/**
 * WorkflowRunContextRepositoryImpl。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRunContextRepositoryImpl implements WorkflowRunContextRepository {

    private final IWorkflowRunContextDao mapper;

    /**
     * upsert。
     *
     * @param ctx 参数
     * @return 返回结果
     */
    @Override
    public WorkflowRunContext upsert(WorkflowRunContext ctx) {
        if (ctx == null) {
            return null;
        }
        mapper.upsert(ctx);
        return ctx;
    }

    /**
     * findByRunId。
     *
     * @param scopeId 参数
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public Optional<WorkflowRunContext> findByRunId(String runId) {
        if (!StringUtils.hasText(runId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByRunId(runId));
    }

    /**
     * updateStatus。
     *
     * @param scopeId 参数
     * @param runId 参数
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public int updateStatus(String runId, String status) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(status)) {
            return 0;
        }
        return mapper.updateStatus(runId, status);
    }

    /**
     * deleteByRunIds。
     *
     * @param scopeId 参数
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
