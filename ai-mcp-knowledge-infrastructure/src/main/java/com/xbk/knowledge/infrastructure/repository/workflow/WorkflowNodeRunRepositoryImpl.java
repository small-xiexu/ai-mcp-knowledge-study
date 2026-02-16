package com.xbk.knowledge.infrastructure.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNodeRun;
import com.xbk.knowledge.domain.repository.workflow.WorkflowNodeRunRepository;
import com.xbk.knowledge.infrastructure.mapper.workflow.WorkflowNodeRunMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowNodeRunRepositoryImpl。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class WorkflowNodeRunRepositoryImpl implements WorkflowNodeRunRepository {

    private final WorkflowNodeRunMapper mapper;

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

    /**
     * insert。
     *
     * @param nodeRun 参数
     * @return 返回结果
     */
    @Override
    public WorkflowNodeRun insert(WorkflowNodeRun nodeRun) {
        if (nodeRun == null) {
            return null;
        }
        mapper.insertNodeRun(nodeRun);
        return nodeRun;
    }

    /**
     * updateById。
     *
     * @param nodeRun 参数
     * @return 返回结果
     */
    @Override
    public int updateById(WorkflowNodeRun nodeRun) {
        if (nodeRun == null || nodeRun.getId() == null) {
            return 0;
        }
        return mapper.updateNodeRun(nodeRun);
    }

    /**
     * findByRunIdAndNodeKey。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param nodeKey 参数
     * @return 返回结果
     */
    @Override
    public Optional<WorkflowNodeRun> findByRunIdAndNodeKey(Long orgId, String runId, String nodeKey) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(nodeKey)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByRunIdAndNodeKey(orgId, runId, nodeKey));
    }

    /**
     * listByRunId。
     *
     * @param orgId 参数
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowNodeRun> listByRunId(Long orgId, String runId) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (!StringUtils.hasText(runId)) {
            return Collections.emptyList();
        }
        return mapper.listByRunId(orgId, runId);
    }

    /**
     * incrementToolCallCount。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param nodeKey 参数
     * @param delta 参数
     * @return 返回结果
     */
    @Override
    public int incrementToolCallCount(Long orgId, String runId, String nodeKey, int delta) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(nodeKey)) {
            return 0;
        }
        return mapper.incrementToolCallCount(orgId, runId, nodeKey, delta);
    }

    /**
     * incrementToolDeniedCount。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param nodeKey 参数
     * @param delta 参数
     * @return 返回结果
     */
    @Override
    public int incrementToolDeniedCount(Long orgId, String runId, String nodeKey, int delta) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(nodeKey)) {
            return 0;
        }
        return mapper.incrementToolDeniedCount(orgId, runId, nodeKey, delta);
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
