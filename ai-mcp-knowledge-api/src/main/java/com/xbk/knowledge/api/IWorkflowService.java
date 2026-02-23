package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowCreateRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowGraphSaveRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowListRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowResponse;
import com.xbk.knowledge.api.dto.workflow.WorkflowUpdateRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowVersionCreateRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowVersionListRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowVersionPublishRequest;
import com.xbk.knowledge.api.dto.workflow.WorkflowVersionResponse;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

import java.util.List;

/**
 * Workflow 管理服务接口
 * 定义 Workflow 控制面管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IWorkflowService {

    /**
     * 按筛选条件分页查询工作流数据。
     *
     * @param request 工作流分页查询参数。
     * @return 返回 WorkflowResponse 分页数据。
     */
    Result<PageResult<WorkflowResponse>> list(WorkflowListRequest request);

    /**
     * 查询工作流详情。
     *
     * @param request 工作流查询参数。
     * @return 返回 WorkflowResponse 数据。
     */
    Result<WorkflowResponse> get(IdRequest request);

    /**
     * 创建工作流数据。
     *
     * @param request 工作流创建参数。
     * @return 返回 WorkflowResponse 数据。
     */
    Result<WorkflowResponse> create(WorkflowCreateRequest request);

    /**
     * 更新工作流数据。
     *
     * @param request 工作流更新参数。
     * @return 返回 WorkflowResponse 数据。
     */
    Result<WorkflowResponse> update(WorkflowUpdateRequest request);

    /**
     * 创建版本。
     *
     * @param request 工作流创建参数。
     * @return 返回 WorkflowVersionResponse 数据。
     */
    Result<WorkflowVersionResponse> createVersion(WorkflowVersionCreateRequest request);

    /**
     * 查询版本列表。
     *
     * @param request 工作流分页查询参数。
     * @return 返回 WorkflowVersionResponse 列表数据。
     */
    Result<List<WorkflowVersionResponse>> listVersions(WorkflowVersionListRequest request);

    /**
     * 查询版本详情。
     *
     * @param request 工作流查询参数。
     * @return 返回 WorkflowVersionResponse 数据。
     */
    Result<WorkflowVersionResponse> getVersion(IdRequest request);

    /**
     * 发布版本。
     *
     * @param request 工作流发布参数。
     * @return 发布结果
     */
    Result<WorkflowVersionResponse> publish(WorkflowVersionPublishRequest request);

    /**
     * 保存流程图配置。
     *
     * @param request 工作流保存参数。
     * @return 返回 WorkflowVersionResponse 数据。
     */
    Result<WorkflowVersionResponse> saveGraph(WorkflowGraphSaveRequest request);
}
