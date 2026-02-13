package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xbk.knowledge.api.dto.role.RoleCreateRequest;
import com.xbk.knowledge.api.dto.role.RolePermissionGrantRequest;
import com.xbk.knowledge.api.dto.role.RolePermissionQueryRequest;
import com.xbk.knowledge.api.dto.role.RoleQueryRequest;
import com.xbk.knowledge.api.dto.role.RoleResponse;
import com.xbk.knowledge.api.dto.role.RoleUpdateRequest;
import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.application.service.app.RoleAppService;
import com.xbk.knowledge.domain.model.entity.SysRole;
import com.xbk.knowledge.domain.model.vo.identity.RolePageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 角色管理接口控制器。
 *
 * 职责：触发层接口适配，用于提供角色管理能力。
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleAppService roleAppService;
    private final AuthAppService authAppService;

    /**
     * 分页查询角色。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @SaCheckPermission("role:read")
    @PostMapping("/list")
    public Result<PageResult<RoleResponse>> list(@Valid @RequestBody RoleQueryRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        RolePageQuery query = new RolePageQuery(
                tenantId,
                request.getRoleCode(),
                request.getStatus(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<SysRole> rolePage = roleAppService.queryRolePage(query);
        PageResult<RoleResponse> responsePage = PageResultConverter.convert(rolePage, this::toResponse);
        return Result.success(responsePage);
    }

    /**
     * 创建角色。
     *
     * @param request 创建请求
     * @return 角色信息
     */
    @SaCheckPermission("role:write")
    @PostMapping("/create")
    public Result<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        SysRole role = SysRole.builder()
                .tenantId(tenantId)
                .roleCode(request.getRoleCode())
                .roleName(request.getRoleName())
                .roleScope(request.getRoleScope())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
        SysRole saved = roleAppService.createRole(role);
        return Result.success("角色创建成功", toResponse(saved));
    }

    /**
     * 更新角色。
     *
     * @param request 更新请求
     * @return 角色信息
     */
    @SaCheckPermission("role:write")
    @PostMapping("/update")
    public Result<RoleResponse> update(@Valid @RequestBody RoleUpdateRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        SysRole role = SysRole.builder()
                .id(request.getId())
                .tenantId(tenantId)
                .roleName(request.getRoleName())
                .roleScope(request.getRoleScope())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
        SysRole updated = roleAppService.updateRole(role);
        return Result.success("角色更新成功", toResponse(updated));
    }

    /**
     * 角色授权。
     *
     * @param request 授权请求
     * @return 响应
     */
    @SaCheckPermission("role:write")
    @PostMapping("/grant-permissions")
    public Result<Void> grantPermissions(@Valid @RequestBody RolePermissionGrantRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        Long operatorId = currentProfile.getUserId();
        roleAppService.grantPermissions(tenantId, request.getRoleId(), request.getPermissionIds(), operatorId);
        return Result.success();
    }

    /**
     * 查询角色已分配权限ID列表。
     *
     * @param request 查询请求
     * @return 权限ID列表
     */
    @SaCheckPermission("role:read")
    @PostMapping("/permission-ids")
    public Result<List<Long>> permissionIds(@Valid @RequestBody RolePermissionQueryRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        List<Long> permissionIds = roleAppService.queryPermissionIds(tenantId, request.getRoleId());
        return Result.success(permissionIds);
    }

    /**
     * 转换角色响应。
     *
     * @param role 角色实体
     * @return 响应 DTO
     */
    private RoleResponse toResponse(SysRole role) {
        return RoleResponse.builder()
                .id(role.getId())
                .tenantId(role.getTenantId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .roleScope(role.getRoleScope())
                .status(role.getStatus())
                .remark(role.getRemark())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    /**
     * 获取当前登录用户画像。
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
