package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IApprovalService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.approval.ApprovalDecisionRequest;
import com.xbk.knowledge.api.dto.approval.ApprovalIdRequest;
import com.xbk.knowledge.api.dto.approval.ApprovalListRequest;
import com.xbk.knowledge.api.dto.approval.ApprovalResponse;
import com.xbk.knowledge.application.service.app.ApprovalAppService;
import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 审批控制面接口。
 *
 * 职责：
 * 1、 审批单查询（列表/详情）
 * 2、 审批通过/拒绝
 * 3、 支撑“方式B”：审批通过后自动继续运行产出 PlatformContractV1
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController implements IApprovalService {

    private final ApprovalAppService approvalAppService;

    /**
     * 分页查询审批单列表。
     * 流程：
     * 1. 进入接口后执行 `tool:approve` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `approvalAppService.list` 查询审批单分页数据。
     * 4. 将领域对象分页结果转换为 `ApprovalResponse` 分页结果。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 分页查询参数
     * @return 审批单分页结果
     */
    @PostMapping("/list")
    @SaCheckPermission("tool:approve")
    @Override
    public Result<PageResult<ApprovalResponse>> list(@Valid @RequestBody ApprovalListRequest request) {
        PageResult<ApprovalRequest> page = approvalAppService.list(request.getStatus(),
                request.getOffset() == null ? 0 : request.getOffset(),
                request.getPageSize() == null ? 20 : request.getPageSize()
        );
        PageResult<ApprovalResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * 查询审批单详情。
     * 流程：
     * 1. 进入接口后执行 `tool:approve` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `approvalAppService.get` 查询审批单详情。
     * 4. 将领域实体转换为 `ApprovalResponse`。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 审批单 ID 请求
     * @return 审批单详情
     */
    @PostMapping("/get")
    @SaCheckPermission("tool:approve")
    @Override
    public Result<ApprovalResponse> get(@Valid @RequestBody ApprovalIdRequest request) {
        ApprovalRequest approval = approvalAppService.get(request.getId());
        return Result.success(toResponse(approval));
    }

    /**
     * 审批通过（方式B：自动续跑）。
     * 流程：
     * 1. 进入接口后执行 `tool:approve` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `approvalAppService.approve` 执行审批通过。
     * 4. 应用层按审批快照触发续跑并生成 `PlatformContractV1` 结果。
     * 5. Controller 统一封装成功文案与续跑结果返回。
     *
     * @param request 审批决策请求
     * @return 续跑后的平台标准结果
     */
    @PostMapping("/approve")
    @SaCheckPermission("tool:approve")
    @Override
    public Result<PlatformContractV1> approve(@Valid @RequestBody ApprovalDecisionRequest request) {
        PlatformContractV1 result = approvalAppService.approve(request.getId(), request.getDecisionComment());
        return Result.success("审批通过并已续跑完成", result);
    }

    /**
     * 审批拒绝。
     * 流程：
     * 1. 进入接口后执行 `tool:approve` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `approvalAppService.reject` 执行审批拒绝。
     * 4. 将拒绝后的审批实体转换为 `ApprovalResponse`。
     * 5. 统一封装拒绝结果并返回。
     *
     * @param request 审批决策请求
     * @return 拒绝后的审批单详情
     */
    @PostMapping("/reject")
    @SaCheckPermission("tool:approve")
    @Override
    public Result<ApprovalResponse> reject(@Valid @RequestBody ApprovalDecisionRequest request) {
        ApprovalRequest result = approvalAppService.reject(request.getId(), request.getDecisionComment());
        return Result.success("审批已拒绝", toResponse(result));
    }

    /**
     * 将输入数据转换为响应。
     *
     * @param approval 审批记录。
     * @return 返回ApprovalResponse对象。
     */
    private ApprovalResponse toResponse(ApprovalRequest approval) {
        if (approval == null) {
            return null;
        }
        return ApprovalResponse.builder()
                .id(approval.getId())
                .approvalType(approval.getApprovalType())
                .status(approval.getStatus())
                .runId(approval.getRunId())
                .agentId(approval.getAgentId())
                .agentVersionId(approval.getAgentVersionId())
                .workflowId(approval.getWorkflowId())
                .workflowVersionId(approval.getWorkflowVersionId())
                .nodeKey(approval.getNodeKey())
                .requesterId(approval.getRequesterId())
                .requesterType(approval.getRequesterType())
                .requestReason(approval.getRequestReason())
                .approverId(approval.getApproverId())
                .decisionComment(approval.getDecisionComment())
                .decidedAt(approval.getDecidedAt())
                .toolKey(approval.getToolKey())
                .riskLevel(approval.getRiskLevel())
                .argumentsDigest(approval.getArgumentsDigest())
                .expireAt(approval.getExpireAt())
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .build();
    }

}
