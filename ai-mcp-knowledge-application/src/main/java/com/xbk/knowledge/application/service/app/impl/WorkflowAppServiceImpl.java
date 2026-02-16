package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.WorkflowAppService;
import com.xbk.knowledge.domain.model.entity.workflow.Workflow;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowEdge;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNode;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowVersion;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowCodeQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionListQuery;
import com.xbk.knowledge.domain.repository.workflow.WorkflowGraphRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowVersionRepository;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Workflow 控制面应用服务实现。
 
  * @author xiexu
  */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAppServiceImpl implements WorkflowAppService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowGraphRepository workflowGraphRepository;
    private final IdentityContextService identityContextService;

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
    public PageResult<Workflow> list(Long orgId, String keyword, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        List<Workflow> list = workflowRepository.list(orgId, keyword, safeOffset, safeSize);
        long total = workflowRepository.count(orgId, keyword);
        int pageNum = safeSize == 0 ? 1 : (safeOffset / safeSize) + 1;
        return PageResult.of(list, total, pageNum, safeSize);
    }

    /**
     * get。
     *
     * @param orgId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Workflow get(Long orgId, Long id) {
        return workflowRepository.findById(new IdQuery(orgId, id))
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，id=" + id));
    }

    /**
     * create。
     *
     * @param orgId 参数
     * @param workflow 参数
     * @return 返回结果
     */
    @Override
    public Workflow create(Long orgId, Workflow workflow) {
        if (orgId == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        if (workflow == null) {
            throw new IllegalArgumentException("workflow 不能为空");
        }
        if (!StringUtils.hasText(workflow.getWorkflowCode()) || !StringUtils.hasText(workflow.getWorkflowName())) {
            throw new BusinessException("workflowCode/workflowName 不能为空");
        }
        workflowRepository.findByCode(WorkflowCodeQuery.builder().orgId(orgId).workflowCode(workflow.getWorkflowCode()).build())
                .ifPresent(existed -> {
                    throw new BusinessException("WorkflowCode 已存在: " + workflow.getWorkflowCode());
                });

        Long operatorId = identityContextService.getCurrentUserId();
        workflow.setOrgId(orgId);
        workflow.setStatus(StringUtils.hasText(workflow.getStatus()) ? workflow.getStatus() : "ENABLED");
        workflow.setCreatedBy(operatorId);
        workflow.setUpdatedBy(operatorId);
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());
        workflowRepository.insert(workflow);
        return workflow;
    }

    /**
     * update。
     *
     * @param orgId 参数
     * @param workflow 参数
     * @return 返回结果
     */
    @Override
    public Workflow update(Long orgId, Workflow workflow) {
        if (orgId == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        if (workflow == null || workflow.getId() == null) {
            throw new IllegalArgumentException("workflow/id 不能为空");
        }
        Workflow existed = get(orgId, workflow.getId());
        existed.setWorkflowName(workflow.getWorkflowName());
        existed.setDescription(workflow.getDescription());
        if (StringUtils.hasText(workflow.getStatus())) {
            existed.setStatus(workflow.getStatus());
        }
        existed.setUpdatedBy(identityContextService.getCurrentUserId());
        existed.setUpdatedAt(LocalDateTime.now());
        int affected = workflowRepository.updateById(existed);
        if (affected <= 0) {
            throw new BusinessException("更新失败，id=" + workflow.getId());
        }
        return get(orgId, workflow.getId());
    }

    /**
     * createVersion。
     *
     * @param orgId 参数
     * @param workflowId 参数
     * @param changeSummary 参数
     * @return 返回结果
     */
    @Override
    public WorkflowVersion createVersion(Long orgId, Long workflowId, String changeSummary) {
        if (orgId == null || workflowId == null) {
            throw new IllegalArgumentException("orgId/workflowId 不能为空");
        }
        Workflow wf = workflowRepository.findById(new IdQuery(orgId, workflowId))
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，id=" + workflowId));
        List<WorkflowVersion> versions = workflowVersionRepository.listByWorkflowId(WorkflowVersionListQuery.builder()
                .orgId(orgId)
                .workflowId(workflowId)
                .build());
        int nextNo = 1;
        if (versions != null && !versions.isEmpty()) {
            Integer max = versions.stream()
                    .map(WorkflowVersion::getVersionNo)
                    .filter(v -> v != null)
                    .max(Integer::compareTo)
                    .orElse(0);
            nextNo = max + 1;
        }
        Long operatorId = identityContextService.getCurrentUserId();
        WorkflowVersion v = WorkflowVersion.builder()
                .orgId(orgId)
                .workflowId(workflowId)
                .versionNo(nextNo)
                .state("DRAFT")
                .changeSummary(changeSummary)
                .graphJson(null)
                .defaultConfigJson(null)
                .createdBy(operatorId)
                .updatedBy(operatorId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        workflowVersionRepository.insert(v);
        log.info("WorkflowVersion created, workflowCode={}, versionNo={}, id={}", wf.getWorkflowCode(), nextNo, v.getId());
        return v;
    }

    /**
     * listVersions。
     *
     * @param orgId 参数
     * @param workflowId 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowVersion> listVersions(Long orgId, Long workflowId) {
        return workflowVersionRepository.listByWorkflowId(WorkflowVersionListQuery.builder()
                .orgId(orgId)
                .workflowId(workflowId)
                .build());
    }

    /**
     * getVersion。
     *
     * @param orgId 参数
     * @param workflowVersionId 参数
     * @return 返回结果
     */
    @Override
    public WorkflowVersion getVersion(Long orgId, Long workflowVersionId) {
        if (orgId == null || workflowVersionId == null) {
            throw new IllegalArgumentException("orgId/workflowVersionId 不能为空");
        }
        return workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().orgId(orgId).id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
    }

    /**
     * publishVersion。
     *
     * @param orgId 参数
     * @param workflowVersionId 参数
     * @return 返回结果
     */
    @Override
    public WorkflowVersion publishVersion(Long orgId, Long workflowVersionId) {
        if (orgId == null || workflowVersionId == null) {
            throw new IllegalArgumentException("orgId/workflowVersionId 不能为空");
        }
        WorkflowVersion v = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().orgId(orgId).id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));

        Workflow wf = workflowRepository.findById(new IdQuery(orgId, v.getWorkflowId()))
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，id=" + v.getWorkflowId()));

        // archive old published
        workflowVersionRepository.findPublishedVersion(orgId, v.getWorkflowId()).ifPresent(old -> {
            if (old.getId() != null && !old.getId().equals(v.getId())) {
                old.setState("ARCHIVED");
                old.setUpdatedBy(identityContextService.getCurrentUserId());
                old.setUpdatedAt(LocalDateTime.now());
                workflowVersionRepository.updateById(old);
            }
        });

        v.setState("PUBLISHED");
        v.setUpdatedBy(identityContextService.getCurrentUserId());
        v.setUpdatedAt(LocalDateTime.now());
        workflowVersionRepository.updateById(v);

        wf.setCurrentPublishedVersionId(v.getId());
        wf.setUpdatedBy(identityContextService.getCurrentUserId());
        wf.setUpdatedAt(LocalDateTime.now());
        workflowRepository.updateById(wf);

        return workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().orgId(orgId).id(v.getId()).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
    }

    @Override
    public WorkflowVersion saveGraph(Long orgId,
                                    Long workflowVersionId,
                                    String graphJson,
                                    String defaultConfigJson,
                                    List<WorkflowNode> nodes,
                                    List<WorkflowEdge> edges) {
        if (orgId == null || workflowVersionId == null) {
            throw new IllegalArgumentException("orgId/workflowVersionId 不能为空");
        }
        WorkflowVersion v = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().orgId(orgId).id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
        if (!"DRAFT".equalsIgnoreCase(v.getState())) {
            throw new BusinessException("仅 DRAFT 版本可编辑图，state=" + v.getState());
        }

        v.setGraphJson(graphJson);
        v.setDefaultConfigJson(defaultConfigJson);
        v.setUpdatedBy(identityContextService.getCurrentUserId());
        v.setUpdatedAt(LocalDateTime.now());
        workflowVersionRepository.updateById(v);

        workflowGraphRepository.replaceGraph(orgId, workflowVersionId, nodes, edges);
        return workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().orgId(orgId).id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
    }
}
