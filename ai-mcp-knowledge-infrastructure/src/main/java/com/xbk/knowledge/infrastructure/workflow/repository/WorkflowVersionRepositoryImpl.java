package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionListQuery;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowVersionRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IWorkflowVersionDao;
import com.xbk.knowledge.infrastructure.dao.po.WorkflowVersionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 查询工作流版本。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowVersionRepositoryImpl implements WorkflowVersionRepository {

    private final IWorkflowVersionDao mapper;

    /**
     * 查询工作流版本。
     *
     * @param query 查询条件
     * @return 返回 WorkflowVersion 查询结果（可能为空）。
     */
    @Override
    public Optional<WorkflowVersion> findById(WorkflowVersionIdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(query))
                .map(item -> BeanMappingUtils.map(item, WorkflowVersion.class));
    }

    /**
     * 根据筛选条件查询工作流版本列表。
     *
     * @param query 查询条件
     * @return 返回 WorkflowVersion 列表数据。
     */
    @Override
    public List<WorkflowVersion> listByWorkflowId(WorkflowVersionListQuery query) {
        if (query == null || query.getWorkflowId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.listByWorkflowId(query), WorkflowVersion.class);
    }

    /**
     * 创建并持久化工作流版本数据。
     *
     * @param version 版本实体。
     * @return 返回 WorkflowVersion 数据。
     */
    @Override
    public WorkflowVersion insert(WorkflowVersion version) {
        if (version == null) {
            return null;
        }
        mapper.insertVersion(BeanMappingUtils.map(version, WorkflowVersionPO.class));
        return version;
    }

    /**
     * 更新工作流版本数据。
     *
     * @param version 版本实体。
     * @return 返回版本更新条数。
     */
    @Override
    public int updateById(WorkflowVersion version) {
        if (version == null || version.getId() == null) {
            return 0;
        }
        return mapper.updateVersion(BeanMappingUtils.map(version, WorkflowVersionPO.class));
    }

    /**
     * 查询工作流版本。
     *
     * @param workflowId Workflow ID
     * @return 返回 WorkflowVersion 查询结果（可能为空）。
     */
    @Override
    public Optional<WorkflowVersion> findPublishedVersion(Long workflowId) {
        if (workflowId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findPublishedVersion(workflowId))
                .map(item -> BeanMappingUtils.map(item, WorkflowVersion.class));
    }
}
