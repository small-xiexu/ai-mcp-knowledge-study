package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.tool.ToolPolicyQueryRequest;
import com.xbk.knowledge.api.dto.tool.ToolPolicyResponse;
import com.xbk.knowledge.api.dto.tool.ToolPolicySaveRequest;
import com.xbk.knowledge.application.service.app.ToolPolicyAppService;
import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import com.xbk.knowledge.domain.model.vo.tool.ToolPolicyPageQuery;
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

/**
 * 工具风险策略控制面接口。
 *
 * 职责：为 tool_policy 提供 CRUD 与启停管理（按 org 隔离）。
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/tool-policies")
@RequiredArgsConstructor
public class ToolPolicyController {

    private final ToolPolicyAppService toolPolicyAppService;

    /**
     * 分页查询工具风险策略列表。
     */
    @PostMapping("/list")
    @SaCheckPermission("tool:read")
    public Result<PageResult<ToolPolicyResponse>> list(@Valid @RequestBody ToolPolicyQueryRequest request) {
        Long orgId = currentOrgId();
        Integer enabled = request.getEnabled() == null ? null : (request.getEnabled() ? 1 : 0);
        ToolPolicyPageQuery query = new ToolPolicyPageQuery(
                orgId,
                request.getKeyword(),
                enabled,
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<ToolPolicy> page = toolPolicyAppService.queryPage(query);
        PageResult<ToolPolicyResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * 查询详情。
     */
    @PostMapping("/get")
    @SaCheckPermission("tool:read")
    public Result<ToolPolicyResponse> get(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        ToolPolicy policy = toolPolicyAppService.get(orgId, request.getId());
        return Result.success(toResponse(policy));
    }

    /**
     * 保存（新增/更新）。
     */
    @PostMapping("/save")
    @SaCheckPermission("tool:write")
    public Result<ToolPolicyResponse> save(@Valid @RequestBody ToolPolicySaveRequest request) {
        Long orgId = currentOrgId();
        ToolPolicy policy = ToolPolicy.builder()
                .id(request.getId())
                .orgId(orgId)
                .toolKey(request.getToolKey())
                .riskLevel(request.getRiskLevel())
                .approvalRequired(request.getApprovalRequired() != null && request.getApprovalRequired() ? 1 : 0)
                .enabled(request.getEnabled() == null || request.getEnabled() ? 1 : 0)
                .remark(request.getRemark())
                .build();
        ToolPolicy saved = toolPolicyAppService.save(policy);
        return Result.success("保存成功", toResponse(saved));
    }

    /**
     * 启用。
     */
    @PostMapping("/enable")
    @SaCheckPermission("tool:write")
    public Result<ToolPolicyResponse> enable(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        ToolPolicy enabled = toolPolicyAppService.enable(orgId, request.getId());
        return Result.success(toResponse(enabled));
    }

    /**
     * 禁用。
     */
    @PostMapping("/disable")
    @SaCheckPermission("tool:write")
    public Result<ToolPolicyResponse> disable(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        ToolPolicy disabled = toolPolicyAppService.disable(orgId, request.getId());
        return Result.success(toResponse(disabled));
    }

    /**
     * 删除。
     */
    @PostMapping("/remove")
    @SaCheckPermission("tool:write")
    public Result<Void> remove(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        toolPolicyAppService.remove(orgId, request.getId());
        return Result.success();
    }

    private ToolPolicyResponse toResponse(ToolPolicy p) {
        if (p == null) {
            return null;
        }
        ToolPolicyResponse resp = new ToolPolicyResponse();
        resp.setId(p.getId());
        resp.setOrgId(p.getOrgId());
        resp.setToolKey(p.getToolKey());
        resp.setRiskLevel(p.getRiskLevel());
        resp.setApprovalRequired(p.getApprovalRequired());
        resp.setEnabled(p.getEnabled());
        resp.setRemark(p.getRemark());
        resp.setCreatedAt(p.getCreatedAt());
        resp.setUpdatedAt(p.getUpdatedAt());
        return resp;
    }

    private Long currentOrgId() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId != null ? orgId : 1L;
    }
}

