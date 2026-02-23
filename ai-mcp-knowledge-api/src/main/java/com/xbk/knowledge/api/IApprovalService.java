package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.approval.ApprovalDecisionRequest;
import com.xbk.knowledge.api.dto.approval.ApprovalIdRequest;
import com.xbk.knowledge.api.dto.approval.ApprovalListRequest;
import com.xbk.knowledge.api.dto.approval.ApprovalResponse;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.contract.PlatformContractV1;

/**
 * 审批服务接口
 * 定义工具审批与决策处理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IApprovalService {

    /**
     * 按筛选条件分页查询审批数据。
     *
     * @param request 审批分页查询参数。
     * @return 返回 ApprovalResponse 分页数据。
     */
    Result<PageResult<ApprovalResponse>> list(ApprovalListRequest request);

    /**
     * 查询审批详情。
     *
     * @param request 审批查询参数。
     * @return 返回 ApprovalResponse 数据。
     */
    Result<ApprovalResponse> get(ApprovalIdRequest request);

    /**
     * 执行审批通过操作。
     *
     * @param request 审批审批参数。
     * @return 审批结果
     */
    Result<PlatformContractV1> approve(ApprovalDecisionRequest request);

    /**
     * 执行审批拒绝操作。
     *
     * @param request 审批审批参数。
     * @return 审批结果
     */
    Result<ApprovalResponse> reject(ApprovalDecisionRequest request);
}
