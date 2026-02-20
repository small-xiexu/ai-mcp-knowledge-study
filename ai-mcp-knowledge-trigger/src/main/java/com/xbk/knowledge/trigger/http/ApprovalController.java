package com.xbk.knowledge.trigger.http;

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
 * 1) 审批单查询（列表/详情）
 * 2) 审批通过/拒绝
 * 3) 支撑“方式B”：审批通过后自动继续运行产出 PlatformContractV1
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalAppService approvalAppService;

    /**
     * 分页查询审批单列表。
     */
    @PostMapping("/list")
    @SaCheckPermission("tool:approve")
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
     */
    @PostMapping("/get")
    @SaCheckPermission("tool:approve")
    public Result<ApprovalResponse> get(@Valid @RequestBody ApprovalIdRequest request) {
        ApprovalRequest approval = approvalAppService.get(request.getId());
        return Result.success(toResponse(approval));
    }

    /**
     * 审批通过（方式B：自动续跑）。
     */
    @PostMapping("/approve")
    @SaCheckPermission("tool:approve")
    public Result<PlatformContractV1> approve(@Valid @RequestBody ApprovalDecisionRequest request) {
        PlatformContractV1 result = approvalAppService.approve(request.getId(), request.getDecisionComment());
        return Result.success("审批通过并已续跑完成", result);
    }

    /**
     * 审批拒绝。
     */
    @PostMapping("/reject")
    @SaCheckPermission("tool:approve")
    public Result<ApprovalResponse> reject(@Valid @RequestBody ApprovalDecisionRequest request) {
        ApprovalRequest result = approvalAppService.reject(request.getId(), request.getDecisionComment());
        return Result.success("审批已拒绝", toResponse(result));
    }

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
