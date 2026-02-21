package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowCodeQuery;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IWorkflowDao;
import com.xbk.knowledge.infrastructure.dao.po.WorkflowPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowRepositoryImpl。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRepositoryImpl implements WorkflowRepository {

    private final IWorkflowDao workflowMapper;

    /**
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<Workflow> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(workflowMapper.findById(query))
                .map(item -> BeanMappingUtils.map(item, Workflow.class));
    }

    /**
     * findByCode。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<Workflow> findByCode(WorkflowCodeQuery query) {
        if (query == null || !StringUtils.hasText(query.getWorkflowCode())) {
            return Optional.empty();
        }
        return Optional.ofNullable(workflowMapper.findByCode(query))
                .map(item -> BeanMappingUtils.map(item, Workflow.class));
    }

    /**
     * insert。
     *
     * @param workflow 参数
     * @return 返回结果
     */
    @Override
    public Workflow insert(Workflow workflow) {
        if (workflow == null) {
            return null;
        }
        workflowMapper.insertWorkflow(BeanMappingUtils.map(workflow, WorkflowPO.class));
        return workflow;
    }

    /**
     * updateById。
     *
     * @param workflow 参数
     * @return 返回结果
     */
    @Override
    public int updateById(Workflow workflow) {
        if (workflow == null || workflow.getId() == null) {
            return 0;
        }
        return workflowMapper.updateWorkflow(BeanMappingUtils.map(workflow, WorkflowPO.class));
    }

    /**
     * list。
     *
     * @param keyword 参数
     * @param offset 参数
     * @param pageSize 参数
     * @return 返回结果
     */
    @Override
    public List<Workflow> list(String keyword, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return BeanMappingUtils.mapList(workflowMapper.list(keyword, safeOffset, safeSize), Workflow.class);
    }

    /**
     * count。
     *
     * @param keyword 参数
     * @return 返回结果
     */
    @Override
    public long count(String keyword) {
        return workflowMapper.count(keyword);
    }
}
