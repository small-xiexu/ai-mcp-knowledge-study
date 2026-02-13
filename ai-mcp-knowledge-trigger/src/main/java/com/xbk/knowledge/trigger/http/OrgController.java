package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xbk.knowledge.api.dto.org.OrgCreateRequest;
import com.xbk.knowledge.api.dto.org.OrgQueryRequest;
import com.xbk.knowledge.api.dto.org.OrgResponse;
import com.xbk.knowledge.api.dto.org.OrgUpdateRequest;
import com.xbk.knowledge.api.dto.org.UserOrgBindRequest;
import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.application.service.app.OrgAppService;
import com.xbk.knowledge.domain.model.entity.SysOrg;
import com.xbk.knowledge.domain.model.vo.identity.OrgQuery;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织管理接口控制器。
 *
 * 职责：触发层接口适配，用于提供组织管理能力。
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrgAppService orgAppService;
    private final AuthAppService authAppService;

    /**
     * 查询组织列表。
     *
     * @param request 查询请求
     * @return 组织列表
     */
    @SaCheckPermission("user:read")
    @PostMapping("/list")
    public Result<List<OrgResponse>> list(@RequestBody(required = false) OrgQueryRequest request) {
        OrgQueryRequest safeRequest = request == null ? OrgQueryRequest.builder().build() : request;
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, safeRequest.getTenantId());
        OrgQuery query = new OrgQuery(tenantId, safeRequest.getStatus());
        List<SysOrg> orgs = orgAppService.queryList(query);
        List<OrgResponse> responses = orgs.stream().map(this::toResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 创建组织。
     *
     * @param request 创建请求
     * @return 组织信息
     */
    @SaCheckPermission("user:write")
    @PostMapping("/create")
    public Result<OrgResponse> create(@Valid @RequestBody OrgCreateRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        SysOrg org = SysOrg.builder()
                .tenantId(tenantId)
                .orgCode(request.getOrgCode())
                .orgName(request.getOrgName())
                .parentId(request.getParentId())
                .orgPath(request.getOrgPath())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
        SysOrg saved = orgAppService.createOrg(org);
        return Result.success("组织创建成功", toResponse(saved));
    }

    /**
     * 更新组织。
     *
     * @param request 更新请求
     * @return 组织信息
     */
    @SaCheckPermission("user:write")
    @PostMapping("/update")
    public Result<OrgResponse> update(@Valid @RequestBody OrgUpdateRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        SysOrg org = SysOrg.builder()
                .id(request.getId())
                .tenantId(tenantId)
                .orgName(request.getOrgName())
                .parentId(request.getParentId())
                .orgPath(request.getOrgPath())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
        SysOrg updated = orgAppService.updateOrg(org);
        return Result.success("组织更新成功", toResponse(updated));
    }

    /**
     * 绑定用户主组织。
     *
     * @param request 绑定请求
     * @return 响应
     */
    @SaCheckPermission("user:write")
    @PostMapping("/bind-user")
    public Result<Void> bindUser(@Valid @RequestBody UserOrgBindRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        orgAppService.bindUserPrimaryOrg(tenantId, request.getUserId(), request.getOrgId());
        return Result.success();
    }

    /**
     * 转换组织响应。
     *
     * @param org 组织实体
     * @return 响应 DTO
     */
    private OrgResponse toResponse(SysOrg org) {
        return OrgResponse.builder()
                .id(org.getId())
                .tenantId(org.getTenantId())
                .orgCode(org.getOrgCode())
                .orgName(org.getOrgName())
                .parentId(org.getParentId())
                .orgPath(org.getOrgPath())
                .status(org.getStatus())
                .remark(org.getRemark())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

    /**
     * 读取当前登录用户画像。
     *
     * @return 用户画像
     */
    private AuthProfile currentProfile() {
        Long loginUserId = StpUtil.getLoginIdAsLong();
        return authAppService.loadProfile(loginUserId);
    }

    /**
     * 解析目标租户ID。
     *
     * @param currentProfile 当前登录用户
     * @param requestedTenantId 请求中的租户ID
     * @return 目标租户ID
     */
    private String resolveTenantId(AuthProfile currentProfile, String requestedTenantId) {
        if (Boolean.TRUE.equals(currentProfile.getSuperAdmin()) && StringUtils.hasText(requestedTenantId)) {
            return requestedTenantId.trim();
        }
        return currentProfile.getTenantId();
    }
}
