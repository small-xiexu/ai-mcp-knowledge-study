package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.workflow.*;
import com.xbk.knowledge.application.service.app.WorkflowAppService;
import com.xbk.knowledge.domain.model.entity.workflow.Workflow;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowEdge;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNode;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowVersion;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Workflow 控制面接口。
 
  * @author xiexu
  */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowAppService workflowAppService;

    /**
     * list。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/list")
    @SaCheckPermission("workflow:read")
    public Result<PageResult<WorkflowResponse>> list(@Valid @RequestBody WorkflowListRequest request) {
        Long orgId = currentOrgId();
        PageResult<Workflow> page = workflowAppService.list(
                orgId,
                request.getKeyword(),
                request.getOffset() == null ? 0 : request.getOffset(),
                request.getPageSize() == null ? 20 : request.getPageSize()
        );
        PageResult<WorkflowResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * get。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/get")
    @SaCheckPermission("workflow:read")
    public Result<WorkflowResponse> get(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        Workflow wf = workflowAppService.get(orgId, request.getId());
        return Result.success(toResponse(wf));
    }

    /**
     * create。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/create")
    @SaCheckPermission("workflow:write")
    public Result<WorkflowResponse> create(@Valid @RequestBody WorkflowCreateRequest request) {
        Long orgId = currentOrgId();
        Workflow wf = Workflow.builder()
                .workflowCode(request.getWorkflowCode())
                .workflowName(request.getWorkflowName())
                .description(request.getDescription())
                .status("ENABLED")
                .build();
        Workflow created = workflowAppService.create(orgId, wf);
        return Result.success(toResponse(created));
    }

    /**
     * update。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/update")
    @SaCheckPermission("workflow:write")
    public Result<WorkflowResponse> update(@Valid @RequestBody WorkflowUpdateRequest request) {
        Long orgId = currentOrgId();
        Workflow wf = Workflow.builder()
                .id(request.getId())
                .workflowName(request.getWorkflowName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
        Workflow updated = workflowAppService.update(orgId, wf);
        return Result.success(toResponse(updated));
    }

    /**
     * createVersion。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/versions/create")
    @SaCheckPermission("workflow:write")
    public Result<WorkflowVersionResponse> createVersion(@Valid @RequestBody WorkflowVersionCreateRequest request) {
        Long orgId = currentOrgId();
        WorkflowVersion v = workflowAppService.createVersion(orgId, request.getWorkflowId(), request.getChangeSummary());
        return Result.success(toVersionResponse(v));
    }

    /**
     * listVersions。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/versions/list")
    @SaCheckPermission("workflow:read")
    public Result<List<WorkflowVersionResponse>> listVersions(@Valid @RequestBody WorkflowVersionListRequest request) {
        Long orgId = currentOrgId();
        List<WorkflowVersion> list = workflowAppService.listVersions(orgId, request.getWorkflowId());
        List<WorkflowVersionResponse> resp = new ArrayList<>();
        if (list != null) {
            for (WorkflowVersion v : list) {
                resp.add(toVersionResponse(v));
            }
        }
        return Result.success(resp);
    }

    /**
     * getVersion。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/versions/get")
    @SaCheckPermission("workflow:read")
    public Result<WorkflowVersionResponse> getVersion(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        WorkflowVersion v = workflowAppService.getVersion(orgId, request.getId());
        return Result.success(toVersionResponse(v));
    }

    /**
     * publish。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/versions/publish")
    @SaCheckPermission("workflow:publish")
    public Result<WorkflowVersionResponse> publish(@Valid @RequestBody WorkflowVersionPublishRequest request) {
        Long orgId = currentOrgId();
        WorkflowVersion v = workflowAppService.publishVersion(orgId, request.getWorkflowVersionId());
        return Result.success("发布成功", toVersionResponse(v));
    }

    /**
     * saveGraph。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/versions/save-graph")
    @SaCheckPermission("workflow:write")
    public Result<WorkflowVersionResponse> saveGraph(@Valid @RequestBody WorkflowGraphSaveRequest request) {
        Long orgId = currentOrgId();

        List<WorkflowNode> nodes = new ArrayList<>();
        if (request.getNodes() != null) {
            for (WorkflowGraphSaveRequest.Node n : request.getNodes()) {
                nodes.add(WorkflowNode.builder()
                        .nodeKey(n.getNodeKey())
                        .nodeType(n.getNodeType())
                        .nodeName(n.getNodeName())
                        .configJson(n.getConfigJson())
                        .positionX(n.getPositionX())
                        .positionY(n.getPositionY())
                        .build());
            }
        }
        List<WorkflowEdge> edges = new ArrayList<>();
        if (request.getEdges() != null) {
            for (WorkflowGraphSaveRequest.Edge e : request.getEdges()) {
                edges.add(WorkflowEdge.builder()
                        .sourceKey(e.getSourceKey())
                        .targetKey(e.getTargetKey())
                        .edgeType(e.getEdgeType())
                        .conditionExpr(e.getConditionExpr())
                        .build());
            }
        }

        WorkflowVersion v = workflowAppService.saveGraph(
                orgId,
                request.getWorkflowVersionId(),
                request.getGraphJson(),
                request.getDefaultConfigJson(),
                nodes,
                edges
        );
        return Result.success("保存成功", toVersionResponse(v));
    }

    private WorkflowResponse toResponse(Workflow wf) {
        if (wf == null) {
            return null;
        }
        return WorkflowResponse.builder()
                .id(wf.getId())
                .orgId(wf.getOrgId())
                .workflowCode(wf.getWorkflowCode())
                .workflowName(wf.getWorkflowName())
                .description(wf.getDescription())
                .status(wf.getStatus())
                .currentPublishedVersionId(wf.getCurrentPublishedVersionId())
                .createdAt(wf.getCreatedAt())
                .updatedAt(wf.getUpdatedAt())
                .build();
    }

    private WorkflowVersionResponse toVersionResponse(WorkflowVersion v) {
        if (v == null) {
            return null;
        }
        return WorkflowVersionResponse.builder()
                .id(v.getId())
                .orgId(v.getOrgId())
                .workflowId(v.getWorkflowId())
                .versionNo(v.getVersionNo())
                .state(v.getState())
                .changeSummary(v.getChangeSummary())
                .graphJson(v.getGraphJson())
                .defaultConfigJson(v.getDefaultConfigJson())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private Long currentOrgId() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId != null ? orgId : 1L;
    }
}
