package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.IWorkflowRuntimeService;
import com.xbk.knowledge.api.dto.workflow.WorkflowNodeRunResponse;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunGetRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunListRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunNodeListRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunResponse;
import com.xbk.knowledge.application.service.app.WorkflowRuntimeAppService;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNodeRun;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRun;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Workflow 运行面接口。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowRuntimeController implements IWorkflowRuntimeService {

    private final WorkflowRuntimeAppService workflowRuntimeAppService;

    /**
     * 执行 Workflow 运行。
     *
     * @param workflowCode Workflow 编码
     * @param request 运行请求
     * @return 平台响应
     */
    @PostMapping("/{workflowCode}/run")
    @SaCheckPermission("workflow:invoke")
    @Override
    public Result<PlatformContractV1> run(@PathVariable("workflowCode") String workflowCode,
                                          @Valid @RequestBody WorkflowRunRequest request) {
        PlatformContractV1 result = workflowRuntimeAppService.run(workflowCode,
                request.getSessionId(),
                request.getContent(),
                request.getVariablesJson(),
                request.getWorkflowVersionId()
        );
        return Result.success(result);
    }

    /**
     * 根据筛选条件查询工作流运行列表。
     *
     * @param request 工作流运行分页查询参数。
     * @return 返回 WorkflowRunResponse 分页数据。
     */
    @PostMapping("/runs/list")
    @SaCheckPermission("workflow:read")
    @Override
    public Result<PageResult<WorkflowRunResponse>> listRuns(@Valid @RequestBody WorkflowRunListRequest request) {
        PageResult<WorkflowRun> page = workflowRuntimeAppService.listRuns(request.getStatus(),
                request.getOffset() == null ? 0 : request.getOffset(),
                request.getPageSize() == null ? 20 : request.getPageSize()
        );
        PageResult<WorkflowRunResponse> responsePage = PageResultConverter.convert(page, this::toRunResponse);
        return Result.success(responsePage);
    }

    /**
     * 查询工作流运行。
     *
     * @param request 工作流运行查询参数。
     * @return 返回 WorkflowRunResponse 数据。
     */
    @PostMapping("/runs/get")
    @SaCheckPermission("workflow:read")
    @Override
    public Result<WorkflowRunResponse> getRun(@Valid @RequestBody WorkflowRunGetRequest request) {
        WorkflowRun run = workflowRuntimeAppService.getRun(request.getRunId());
        return Result.success(toRunResponse(run));
    }

    /**
     * 根据筛选条件查询工作流运行列表。
     *
     * @param request 工作流运行分页查询参数。
     * @return 返回 WorkflowNodeRunResponse 列表数据。
     */
    @PostMapping("/runs/nodes")
    @SaCheckPermission("workflow:read")
    @Override
    public Result<List<WorkflowNodeRunResponse>> listNodeRuns(@Valid @RequestBody WorkflowRunNodeListRequest request) {
        List<WorkflowNodeRun> list = workflowRuntimeAppService.listNodeRuns(request.getRunId());
        return Result.success(list.stream().map(this::toNodeRunResponse).toList());
    }

    private WorkflowRunResponse toRunResponse(WorkflowRun run) {
        if (run == null) {
            return null;
        }
        WorkflowRunResponse response = new WorkflowRunResponse();
        response.setRunId(run.getRunId());
        response.setWorkflowId(run.getWorkflowId());
        response.setWorkflowCode(run.getWorkflowCode());
        response.setWorkflowVersionId(run.getWorkflowVersionId());
        response.setTriggerSource(run.getTriggerSource());
        response.setOperatorId(run.getOperatorId());
        response.setOperatorType(run.getOperatorType());
        response.setSessionId(run.getSessionId());
        response.setStatus(run.getStatus());
        response.setCurrentNodeKey(run.getCurrentNodeKey());
        response.setCostMs(run.getCostMs());
        response.setErrorMessage(run.getErrorMessage());
        response.setStartedAt(run.getStartedAt());
        response.setEndedAt(run.getEndedAt());
        response.setCreatedAt(run.getCreatedAt());
        response.setUpdatedAt(run.getUpdatedAt());
        return response;
    }

    private WorkflowNodeRunResponse toNodeRunResponse(WorkflowNodeRun nodeRun) {
        if (nodeRun == null) {
            return null;
        }
        WorkflowNodeRunResponse response = new WorkflowNodeRunResponse();
        response.setId(nodeRun.getId());
        response.setRunId(nodeRun.getRunId());
        response.setNodeKey(nodeRun.getNodeKey());
        response.setNodeType(nodeRun.getNodeType());
        response.setNodeName(nodeRun.getNodeName());
        response.setStatus(nodeRun.getStatus());
        response.setModelIdUsed(nodeRun.getModelIdUsed());
        response.setModelNameUsed(nodeRun.getModelNameUsed());
        response.setPromptTokens(nodeRun.getPromptTokens());
        response.setCompletionTokens(nodeRun.getCompletionTokens());
        response.setTotalTokens(nodeRun.getTotalTokens());
        response.setToolCallCount(nodeRun.getToolCallCount());
        response.setToolDeniedCount(nodeRun.getToolDeniedCount());
        response.setInputDigest(nodeRun.getInputDigest());
        response.setOutputDigest(nodeRun.getOutputDigest());
        response.setOutputText(nodeRun.getOutputText());
        response.setOutputTruncated(nodeRun.getOutputTruncated());
        response.setApprovalRequestId(nodeRun.getApprovalRequestId());
        response.setCostMs(nodeRun.getCostMs());
        response.setErrorMessage(nodeRun.getErrorMessage());
        response.setStartedAt(nodeRun.getStartedAt());
        response.setEndedAt(nodeRun.getEndedAt());
        response.setCreatedAt(nodeRun.getCreatedAt());
        response.setUpdatedAt(nodeRun.getUpdatedAt());
        return response;
    }
}
