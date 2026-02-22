package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.workflow.WorkflowRunGetRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunListRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunNodeListRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowNodeRunResponse;
import com.xbk.knowledge.api.dto.workflow.WorkflowRunResponse;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.contract.PlatformContractV1;

import java.util.List;

/**
 * Workflow 运行服务接口
 * 定义 Workflow 运行面能力的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IWorkflowRuntimeService {

    /**
     * 执行运行调用。
     *
     * @param workflowCode Workflow 编码
     * @param request 请求参数
     * @return 调用结果
     */
    Result<PlatformContractV1> run(String workflowCode, WorkflowRunRequest request);

    /**
     * 分页查询运行记录。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<WorkflowRunResponse>> listRuns(WorkflowRunListRequest request);

    /**
     * 查询运行详情。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<WorkflowRunResponse> getRun(WorkflowRunGetRequest request);

    /**
     * 查询节点运行明细。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<List<WorkflowNodeRunResponse>> listNodeRuns(WorkflowRunNodeListRequest request);
}
