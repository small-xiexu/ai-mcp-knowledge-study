package com.xbk.knowledge.infrastructure.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRunContext;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRunContextRepository;
import com.xbk.knowledge.infrastructure.mapper.workflow.WorkflowRunContextMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
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

    private final WorkflowRunContextMapper mapper;

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

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
        if (ctx.getOrgId() == null) {
            ctx.setOrgId(currentOrgIdOrRoot());
        }
        mapper.upsert(ctx);
        return ctx;
    }

    /**
     * findByRunId。
     *
     * @param orgId 参数
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public Optional<WorkflowRunContext> findByRunId(Long orgId, String runId) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (!StringUtils.hasText(runId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByRunId(orgId, runId));
    }

    /**
     * updateStatus。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public int updateStatus(Long orgId, String runId, String status) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(status)) {
            return 0;
        }
        return mapper.updateStatus(orgId, runId, status);
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
