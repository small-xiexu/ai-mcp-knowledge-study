package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionListQuery;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowVersionRepository;
import com.xbk.knowledge.infrastructure.dao.IWorkflowVersionDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowVersionRepositoryImpl。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowVersionRepositoryImpl implements WorkflowVersionRepository {

    private final IWorkflowVersionDao mapper;

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
     * @param scopeId 参数
     * @param workflowId 参数
     * @return 返回结果
     */
    @Override
    public Optional<WorkflowVersion> findPublishedVersion(Long workflowId) {
        if (workflowId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findPublishedVersion(workflowId));
    }
}
