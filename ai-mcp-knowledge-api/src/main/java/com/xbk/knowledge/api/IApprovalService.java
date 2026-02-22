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
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<ApprovalResponse>> list(ApprovalListRequest request);

    /**
     * 查询详情信息。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<ApprovalResponse> get(ApprovalIdRequest request);

    /**
     * 执行审批通过操作。
     *
     * @param request 请求参数
     * @return 审批处理结果
     */
    Result<PlatformContractV1> approve(ApprovalDecisionRequest request);

    /**
     * 执行审批拒绝操作。
     *
     * @param request 请求参数
     * @return 审批处理结果
     */
    Result<ApprovalResponse> reject(ApprovalDecisionRequest request);
}
