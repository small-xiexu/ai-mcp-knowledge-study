package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunListRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunGetRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunNodeListRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunRequest;
import com.xbk.knowledge.application.service.app.WorkflowRuntimeAppService;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNodeRun;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRun;
import com.xbk.knowledge.types.common.PageResult;
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
 * @author xiexu
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowRuntimeController {

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
     * listRuns。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/runs/list")
    @SaCheckPermission("workflow:read")
    public Result<PageResult<WorkflowRun>> listRuns(@Valid @RequestBody WorkflowRunListRequest request) {
        PageResult<WorkflowRun> page = workflowRuntimeAppService.listRuns(request.getStatus(),
                request.getOffset() == null ? 0 : request.getOffset(),
                request.getPageSize() == null ? 20 : request.getPageSize()
        );
        return Result.success(page);
    }

    /**
     * getRun。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/runs/get")
    @SaCheckPermission("workflow:read")
    public Result<WorkflowRun> getRun(@Valid @RequestBody WorkflowRunGetRequest request) {
        WorkflowRun run = workflowRuntimeAppService.getRun(request.getRunId());
        return Result.success(run);
    }

    /**
     * listNodeRuns。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/runs/nodes")
    @SaCheckPermission("workflow:read")
    public Result<List<WorkflowNodeRun>> listNodeRuns(@Valid @RequestBody WorkflowRunNodeListRequest request) {
        List<WorkflowNodeRun> list = workflowRuntimeAppService.listNodeRuns(request.getRunId());
        return Result.success(list);
    }

}
