package com.xbk.knowledge.infrastructure.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowVersion;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionListQuery;
import com.xbk.knowledge.domain.repository.workflow.WorkflowVersionRepository;
import com.xbk.knowledge.infrastructure.mapper.workflow.WorkflowVersionMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowVersionRepositoryImpl。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class WorkflowVersionRepositoryImpl implements WorkflowVersionRepository {

    private final WorkflowVersionMapper mapper;

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

    /**
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<WorkflowVersion> findById(WorkflowVersionIdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return Optional.ofNullable(mapper.findById(query));
    }

    /**
     * listByWorkflowId。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowVersion> listByWorkflowId(WorkflowVersionListQuery query) {
        if (query == null || query.getWorkflowId() == null) {
            return Collections.emptyList();
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return mapper.listByWorkflowId(query);
    }

    /**
     * insert。
     *
     * @param version 参数
     * @return 返回结果
     */
    @Override
    public WorkflowVersion insert(WorkflowVersion version) {
        if (version == null) {
            return null;
        }
        mapper.insertVersion(version);
        return version;
    }

    /**
     * updateById。
     *
     * @param version 参数
     * @return 返回结果
     */
    @Override
    public int updateById(WorkflowVersion version) {
        if (version == null || version.getId() == null) {
            return 0;
        }
        return mapper.updateVersion(version);
    }

    /**
     * findPublishedVersion。
     *
     * @param orgId 参数
     * @param workflowId 参数
     * @return 返回结果
     */
    @Override
    public Optional<WorkflowVersion> findPublishedVersion(Long orgId, Long workflowId) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (workflowId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findPublishedVersion(orgId, workflowId));
    }
}

