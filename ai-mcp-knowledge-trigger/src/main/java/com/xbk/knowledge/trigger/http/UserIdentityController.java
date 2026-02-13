package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xbk.knowledge.api.dto.user.UserCreateRequest;
import com.xbk.knowledge.api.dto.user.UserQueryRequest;
import com.xbk.knowledge.api.dto.user.UserRoleGrantRequest;
import com.xbk.knowledge.api.dto.user.UserRoleQueryRequest;
import com.xbk.knowledge.api.dto.user.UserResponse;
import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.application.service.app.UserIdentityAppService;
import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.model.vo.identity.UserPageQuery;
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
 * 用户管理接口控制器。
 *
 * 职责：触发层接口适配，用于提供用户管理能力。
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserIdentityController {

    private final UserIdentityAppService userIdentityAppService;
    private final AuthAppService authAppService;

    /**
     * 用户列表查询。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @SaCheckPermission("user:read")
    @PostMapping("/list")
    public Result<PageResult<UserResponse>> list(@Valid @RequestBody UserQueryRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        UserPageQuery query = new UserPageQuery(
                tenantId,
                request.getUsername(),
                request.getStatus(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<SysUser> userPage = userIdentityAppService.queryUserPage(query);
        PageResult<UserResponse> responsePage = PageResultConverter.convert(userPage, this::toUserResponse);
        return Result.success(responsePage);
    }

    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @return 创建后的用户
     */
    @SaCheckPermission("user:write")
    @PostMapping("/create")
    public Result<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        Integer isSuperAdmin = Boolean.TRUE.equals(request.getSuperAdmin()) ? 1 : 0;
        SysUser user = SysUser.builder()
                .tenantId(tenantId)
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .status(request.getStatus())
                .isSuperAdmin(isSuperAdmin)
                .build();
        SysUser saved = userIdentityAppService.createUser(user, request.getPassword());
        return Result.success("用户创建成功", toUserResponse(saved));
    }

    /**
     * 分配用户角色。
     *
     * @param request 角色分配请求
     * @return 响应
     */
    @SaCheckPermission("user:write")
    @PostMapping("/grant-roles")
    public Result<Void> grantRoles(@Valid @RequestBody UserRoleGrantRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        Long operatorId = currentProfile.getUserId();
        userIdentityAppService.grantRoles(tenantId, request.getUserId(), request.getRoleIds(), operatorId);
        return Result.success();
    }

    /**
     * 查询用户已分配角色ID列表。
     *
     * @param request 查询请求
     * @return 角色ID列表
     */
    @SaCheckPermission("user:read")
    @PostMapping("/role-ids")
    public Result<List<Long>> roleIds(@Valid @RequestBody UserRoleQueryRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        List<Long> roleIds = userIdentityAppService.queryRoleIds(tenantId, request.getUserId());
        return Result.success(roleIds);
    }

    /**
     * 转换用户响应。
     *
     * @param user 用户实体
     * @return 响应 DTO
     */
    private UserResponse toUserResponse(SysUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .status(user.getStatus())
                .superAdmin(user.getIsSuperAdmin() != null && user.getIsSuperAdmin() == 1)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
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
