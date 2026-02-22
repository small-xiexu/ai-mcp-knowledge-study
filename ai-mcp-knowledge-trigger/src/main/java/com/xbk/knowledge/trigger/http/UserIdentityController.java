package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IUserIdentityService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.user.UserCreateRequest;
import com.xbk.knowledge.api.dto.user.UserPasswordResetRequest;
import com.xbk.knowledge.api.dto.user.UserQueryRequest;
import com.xbk.knowledge.api.dto.user.UserRoleGrantRequest;
import com.xbk.knowledge.api.dto.user.UserRoleQueryRequest;
import com.xbk.knowledge.api.dto.user.UserResponse;
import com.xbk.knowledge.api.dto.user.UserUpdateRequest;
import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.UserIdentityAppService;
import com.xbk.knowledge.domain.identity.model.entity.SysUser;
import com.xbk.knowledge.domain.identity.model.valobj.UserPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
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
 * @author sxie
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserIdentityController implements IUserIdentityService {

    private final UserIdentityAppService userIdentityAppService;
    private final AuthAppService authAppService;
    private final IdentityContextService identityContextService;

    /**
     * 用户列表查询。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @SaCheckPermission("user:read")
    @PostMapping("/list")
    @Override
    public Result<PageResult<UserResponse>> list(@Valid @RequestBody UserQueryRequest request) {
        UserPageQuery query = new UserPageQuery(
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
    @Override
    public Result<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        Integer isSuperAdmin = Boolean.TRUE.equals(request.getSuperAdmin()) ? 1 : 0;
        SysUser user = SysUser.builder()
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
     * 更新用户基础信息。
     *
     * @param request 更新请求
     * @return 更新后的用户
     */
    @SaCheckPermission("user:write")
    @PostMapping("/update")
    @Override
    public Result<UserResponse> update(@Valid @RequestBody UserUpdateRequest request) {
        Integer isSuperAdmin = request.getSuperAdmin() == null ? null : (Boolean.TRUE.equals(request.getSuperAdmin()) ? 1 : 0);
        SysUser user = SysUser.builder()
                .id(request.getId())
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .status(request.getStatus())
                .isSuperAdmin(isSuperAdmin)
                .build();
        SysUser updated = userIdentityAppService.updateUser(user);
        return Result.success("用户更新成功", toUserResponse(updated));
    }

    /**
     * 重置用户密码。
     *
     * @param request 密码重置请求
     * @return 响应
     */
    @SaCheckPermission("user:write")
    @PostMapping("/reset-password")
    @Override
    public Result<Void> resetPassword(@Valid @RequestBody UserPasswordResetRequest request) {
        userIdentityAppService.resetPassword(request.getUserId(), request.getPassword());
        return Result.success();
    }

    /**
     * 分配用户角色。
     *
     * @param request 角色分配请求
     * @return 响应
     */
    @SaCheckPermission("user:write")
    @PostMapping("/grant-roles")
    @Override
    public Result<Void> grantRoles(@Valid @RequestBody UserRoleGrantRequest request) {
        AuthProfile currentProfile = currentProfile();
        userIdentityAppService.grantRoles(request.getUserId(), request.getRoleIds(), currentProfile.getUserId());
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
    @Override
    public Result<List<Long>> roleIds(@Valid @RequestBody UserRoleQueryRequest request) {
        List<Long> roleIds = userIdentityAppService.queryRoleIds(request.getUserId());
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
        Long loginUserId = identityContextService.getCurrentUserId();
        return authAppService.loadProfile(loginUserId);
    }
}
