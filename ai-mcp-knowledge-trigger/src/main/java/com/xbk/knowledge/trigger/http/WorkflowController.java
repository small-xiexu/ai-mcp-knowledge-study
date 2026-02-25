package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IWorkflowService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.workflow.*;
import com.xbk.knowledge.application.service.app.WorkflowAppService;
import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowEdge;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
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
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController implements IWorkflowService {

    /**
     * Workflow 应用服务。
     */
    private final WorkflowAppService workflowAppService;

    /**
     * 分页查询工作流列表。
     * 流程：
     * 1. 进入接口后执行 `workflow:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 兜底分页参数并调用 `workflowAppService.list` 查询分页数据。
     * 4. 将领域分页结果转换为 `WorkflowResponse` 分页结构。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 工作流分页查询参数。
     * @return WorkflowResponse 分页数据。
     */
    @PostMapping("/list")
    @SaCheckPermission("workflow:read")
    @Override
    public Result<PageResult<WorkflowResponse>> list(@Valid @RequestBody WorkflowListRequest request) {
        PageResult<Workflow> page = workflowAppService.list(request.getKeyword(),
                request.getOffset() == null ? 0 : request.getOffset(),
                request.getPageSize() == null ? 20 : request.getPageSize()
        );
        PageResult<WorkflowResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * 按主键查询工作流详情。
     * 流程：
     * 1. 进入接口后执行 `workflow:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `workflowAppService.get` 查询领域实体。
     * 4. 将领域对象转换为 `WorkflowResponse`。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 工作流查询参数。
     * @return WorkflowResponse 数据。
     */
    @PostMapping("/get")
    @SaCheckPermission("workflow:read")
    @Override
    public Result<WorkflowResponse> get(@Valid @RequestBody IdRequest request) {
        Workflow wf = workflowAppService.get(request.getId());
        return Result.success(toResponse(wf));
    }

    /**
     * 创建工作流基础信息。
     * 流程：
     * 1. 进入接口后执行 `workflow:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 将请求参数组装为 `Workflow` 并设置默认状态。
     * 4. 调用 `workflowAppService.create` 完成持久化。
     * 5. 转换为 `WorkflowResponse` 并统一封装返回。
     * 
     * @param request 工作流创建参数。
     * @return WorkflowResponse 数据。
     */
    @PostMapping("/create")
    @SaCheckPermission("workflow:write")
    @Override
    public Result<WorkflowResponse> create(@Valid @RequestBody WorkflowCreateRequest request) {
        Workflow wf = Workflow.builder()
                .workflowCode(request.getWorkflowCode())
                .workflowName(request.getWorkflowName())
                .description(request.getDescription())
                .status("ENABLED")
                .build();
        Workflow created = workflowAppService.create(wf);
        return Result.success(toResponse(created));
    }

    /**
     * 更新工作流基础信息。
     * 流程：
     * 1. 进入接口后执行 `workflow:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 基于请求组装更新对象（含 id/status）。
     * 4. 调用 `workflowAppService.update` 执行更新。
     * 5. 转换为 `WorkflowResponse` 并统一封装返回。
     * 
     * @param request 工作流更新参数。
     * @return WorkflowResponse 数据。
     */
    @PostMapping("/update")
    @SaCheckPermission("workflow:write")
    @Override
    public Result<WorkflowResponse> update(@Valid @RequestBody WorkflowUpdateRequest request) {
        Workflow wf = Workflow.builder()
                .id(request.getId())
                .workflowName(request.getWorkflowName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
        Workflow updated = workflowAppService.update(wf);
        return Result.success(toResponse(updated));
    }

    /**
     * 基于工作流创建新版本。
     * 流程：
     * 1. 进入接口后执行 `workflow:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `workflowAppService.createVersion` 创建版本。
     * 4. 将版本实体转换为 `WorkflowVersionResponse`。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 工作流创建参数。
     * @return WorkflowVersionResponse 数据。
     */
    @PostMapping("/versions/create")
    @SaCheckPermission("workflow:write")
    @Override
    public Result<WorkflowVersionResponse> createVersion(@Valid @RequestBody WorkflowVersionCreateRequest request) {
        WorkflowVersion v = workflowAppService.createVersion(request.getWorkflowId(), request.getChangeSummary());
        return Result.success(toVersionResponse(v));
    }

    /**
     * 查询指定工作流的版本列表。
     * 流程：
     * 1. 进入接口后执行 `workflow:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `workflowAppService.listVersions` 查询版本集合。
     * 4. 循环转换为 `WorkflowVersionResponse` 列表。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 工作流分页查询参数。
     * @return WorkflowVersionResponse 列表数据。
     */
    @PostMapping("/versions/list")
    @SaCheckPermission("workflow:read")
    @Override
    public Result<List<WorkflowVersionResponse>> listVersions(@Valid @RequestBody WorkflowVersionListRequest request) {
        List<WorkflowVersion> list = workflowAppService.listVersions(request.getWorkflowId());
        List<WorkflowVersionResponse> resp = new ArrayList<>();
        if (list != null) {
            for (WorkflowVersion v : list) {
                resp.add(toVersionResponse(v));
            }
        }
        return Result.success(resp);
    }

    /**
     * 按版本主键查询版本详情。
     * 流程：
     * 1. 进入接口后执行 `workflow:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `workflowAppService.getVersion` 查询版本实体。
     * 4. 将版本实体转换为 `WorkflowVersionResponse`。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 工作流查询参数。
     * @return WorkflowVersionResponse 数据。
     */
    @PostMapping("/versions/get")
    @SaCheckPermission("workflow:read")
    @Override
    public Result<WorkflowVersionResponse> getVersion(@Valid @RequestBody IdRequest request) {
        WorkflowVersion v = workflowAppService.getVersion(request.getId());
        return Result.success(toVersionResponse(v));
    }

    /**
     * 发布工作流版本。
     * 流程：
     * 1. 进入接口后执行 `workflow:publish` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `workflowAppService.publishVersion` 执行发布。
     * 4. 将发布后的版本实体转换为 `WorkflowVersionResponse`。
     * 5. 返回“发布成功”的统一结果。
     * 
     * @param request 工作流发布参数。
     * @return WorkflowVersionResponse 数据。
     */
    @PostMapping("/versions/publish")
    @SaCheckPermission("workflow:publish")
    @Override
    public Result<WorkflowVersionResponse> publish(@Valid @RequestBody WorkflowVersionPublishRequest request) {
        WorkflowVersion v = workflowAppService.publishVersion(request.getWorkflowVersionId());
        return Result.success("发布成功", toVersionResponse(v));
    }

    /**
     * 保存工作流画布（节点/连线/默认配置）。
     * 流程：
     * 1. 进入接口后执行 `workflow:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 将节点与边 DTO 逐条转换为领域 `WorkflowNode/WorkflowEdge`。
     * 4. 调用 `workflowAppService.saveGraph` 完成图结构持久化。
     * 5. 将结果转换为 `WorkflowVersionResponse` 并返回“保存成功”。
     * 
     * @param request 工作流保存参数。
     * @return WorkflowVersionResponse 数据。
     */
    @PostMapping("/versions/save-graph")
    @SaCheckPermission("workflow:write")
    @Override
    public Result<WorkflowVersionResponse> saveGraph(@Valid @RequestBody WorkflowGraphSaveRequest request) {
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

        WorkflowVersion v = workflowAppService.saveGraph(request.getWorkflowVersionId(),
                request.getGraphJson(),
                request.getDefaultConfigJson(),
                nodes,
                edges
        );
        return Result.success("保存成功", toVersionResponse(v));
    }

    /**
     * 将输入数据转换为响应。
     * 
     * @param wf 工作流定义实体。
     * @return 工作流响应。
     */
    private WorkflowResponse toResponse(Workflow wf) {
        if (wf == null) {
            return null;
        }
        return WorkflowResponse.builder()
                .id(wf.getId())
                .workflowCode(wf.getWorkflowCode())
                .workflowName(wf.getWorkflowName())
                .description(wf.getDescription())
                .status(wf.getStatus())
                .currentPublishedVersionId(wf.getCurrentPublishedVersionId())
                .createdAt(wf.getCreatedAt())
                .updatedAt(wf.getUpdatedAt())
                .build();
    }

    /**
     * 将输入数据转换为版本响应。
     * 
     * @param v 工作流版本实体。
     * @return 工作流版本响应。
     */
    private WorkflowVersionResponse toVersionResponse(WorkflowVersion v) {
        if (v == null) {
            return null;
        }
        return WorkflowVersionResponse.builder()
                .id(v.getId())
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

}
