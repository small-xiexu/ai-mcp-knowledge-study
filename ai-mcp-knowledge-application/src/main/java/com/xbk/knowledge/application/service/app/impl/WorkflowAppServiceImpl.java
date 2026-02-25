package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.WorkflowAppService;
import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowEdge;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowCodeQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionListQuery;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowGraphRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowVersionRepository;
import com.xbk.knowledge.types.common.PageResult;
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
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAppServiceImpl implements WorkflowAppService {

    /**
     * Workflow 仓储。
     */
    private final WorkflowRepository workflowRepository;

    /**
     * Workflow 版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * Workflow 图仓储。
     */
    private final WorkflowGraphRepository workflowGraphRepository;

    /**
     * 当前用户身份上下文服务。
     */
    private final IdentityContextService identityContextService;

    /**
     * 根据筛选条件查询工作流列表。
     *
     * @param keyword 关键字
     * @param offset 分页偏移量
     * @param pageSize 分页大小
     * @return Workflow 分页数据
     */
    @Override
    public PageResult<Workflow> list(String keyword, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        List<Workflow> list = workflowRepository.list(keyword, safeOffset, safeSize);
        long total = workflowRepository.count(keyword);
        int pageNum = safeSize == 0 ? 1 : (safeOffset / safeSize) + 1;
        return PageResult.of(list, total, pageNum, safeSize);
    }

    /**
     * 查询工作流。
     *
     * @param id 主键 ID
     * @return Workflow 详情
     */
    @Override
    public Workflow get(Long id) {
        return workflowRepository.findById(new IdQuery(id))
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，id=" + id));
    }

    /**
     * 创建并持久化工作流数据。
     *
     * @param workflow 工作流实体
     * @return 创建后的 Workflow 信息
     */
    @Override
    public Workflow create(Workflow workflow) {
        if (workflow == null) {
            throw new IllegalArgumentException("workflow 不能为空");
        }
        if (!StringUtils.hasText(workflow.getWorkflowCode()) || !StringUtils.hasText(workflow.getWorkflowName())) {
            throw new BusinessException("workflowCode/workflowName 不能为空");
        }
        workflowRepository.findByCode(WorkflowCodeQuery.builder().workflowCode(workflow.getWorkflowCode()).build())
                .ifPresent(existed -> {
                    throw new BusinessException("WorkflowCode 已存在: " + workflow.getWorkflowCode());
                });

        Long operatorId = identityContextService.getCurrentUserId();
        workflow.setStatus(StringUtils.hasText(workflow.getStatus()) ? workflow.getStatus() : "ENABLED");
        workflow.setCreatedBy(operatorId);
        workflow.setUpdatedBy(operatorId);
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());
        workflowRepository.insert(workflow);
        return workflow;
    }

    /**
     * 更新工作流数据。
     *
     * @param workflow 工作流实体
     * @return 更新后的 Workflow 信息
     */
    @Override
    public Workflow update(Workflow workflow) {
        if (workflow == null || workflow.getId() == null) {
            throw new IllegalArgumentException("workflow/id 不能为空");
        }
        Workflow existed = get(workflow.getId());
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
        return get(workflow.getId());
    }

    /**
     * 创建并持久化工作流数据。
     *
     * @param workflowId Workflow ID
     * @param changeSummary 变更说明
     * @return 新建的 WorkflowVersion 信息
     */
    @Override
    public WorkflowVersion createVersion(Long workflowId, String changeSummary) {
        if (workflowId == null) {
            throw new IllegalArgumentException("workflowId 不能为空");
        }
        Workflow wf = workflowRepository.findById(new IdQuery(workflowId))
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，id=" + workflowId));
        List<WorkflowVersion> versions = workflowVersionRepository.listByWorkflowId(WorkflowVersionListQuery.builder()
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
     * 根据筛选条件查询工作流列表。
     *
     * @param workflowId Workflow ID
     * @return WorkflowVersion 列表
     */
    @Override
    public List<WorkflowVersion> listVersions(Long workflowId) {
        return workflowVersionRepository.listByWorkflowId(WorkflowVersionListQuery.builder()
                .workflowId(workflowId)
                .build());
    }

    /**
     * 查询工作流。
     *
     * @param workflowVersionId 工作流版本 ID
     * @return WorkflowVersion 详情
     */
    @Override
    public WorkflowVersion getVersion(Long workflowVersionId) {
        if (workflowVersionId == null) {
            throw new IllegalArgumentException("workflowVersionId 不能为空");
        }
        return workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
    }

    /**
     * 发布业务配置。
     *
     * @param workflowVersionId 工作流版本 ID
     * @return 发布后的 WorkflowVersion 信息
     */
    @Override
    public WorkflowVersion publishVersion(Long workflowVersionId) {
        if (workflowVersionId == null) {
            throw new IllegalArgumentException("workflowVersionId 不能为空");
        }
        WorkflowVersion v = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));

        Workflow wf = workflowRepository.findById(new IdQuery(v.getWorkflowId()))
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，id=" + v.getWorkflowId()));

        // archive old published
        workflowVersionRepository.findPublishedVersion(v.getWorkflowId()).ifPresent(old -> {
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

        return workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().id(v.getId()).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
    }

    /**
     * 保存流程图。
     * 
     * @param workflowVersionId 工作流版本ID。
     * @param graphJson 流程图JSON。
     * @param defaultConfigJson 默认配置JSON。
     * @param nodes 节点列表。
     * @param edges 边定义列表。
     * @return 保存后的工作流版本。
     */
    @Override
    public WorkflowVersion saveGraph(Long workflowVersionId,
                                    String graphJson,
                                    String defaultConfigJson,
                                    List<WorkflowNode> nodes,
                                    List<WorkflowEdge> edges) {
        if (workflowVersionId == null) {
            throw new IllegalArgumentException("workflowVersionId 不能为空");
        }
        WorkflowVersion v = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
        if (!"DRAFT".equalsIgnoreCase(v.getState())) {
            throw new BusinessException("仅 DRAFT 版本可编辑图，state=" + v.getState());
        }

        v.setGraphJson(graphJson);
        v.setDefaultConfigJson(defaultConfigJson);
        v.setUpdatedBy(identityContextService.getCurrentUserId());
        v.setUpdatedAt(LocalDateTime.now());
        workflowVersionRepository.updateById(v);

        workflowGraphRepository.replaceGraph(workflowVersionId, nodes, edges);
        return workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
    }
}
