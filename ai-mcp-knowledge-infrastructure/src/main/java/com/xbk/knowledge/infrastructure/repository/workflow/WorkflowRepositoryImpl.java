package com.xbk.knowledge.infrastructure.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.Workflow;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowCodeQuery;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRepository;
import com.xbk.knowledge.infrastructure.mapper.workflow.WorkflowMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowRepositoryImpl。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRepositoryImpl implements WorkflowRepository {

    private final WorkflowMapper workflowMapper;

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
    public Optional<Workflow> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return Optional.ofNullable(workflowMapper.findById(query));
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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return Optional.ofNullable(workflowMapper.findByCode(query));
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
        workflowMapper.insertWorkflow(workflow);
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
        return workflowMapper.updateWorkflow(workflow);
    }

    /**
     * list。
     *
     * @param orgId 参数
     * @param keyword 参数
     * @param offset 参数
     * @param pageSize 参数
     * @return 返回结果
     */
    @Override
    public List<Workflow> list(Long orgId, String keyword, int offset, int pageSize) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return workflowMapper.list(orgId, keyword, safeOffset, safeSize);
    }

    /**
     * count。
     *
     * @param orgId 参数
     * @param keyword 参数
     * @return 返回结果
     */
    @Override
    public long count(Long orgId, String keyword) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        return workflowMapper.count(orgId, keyword);
    }
}

