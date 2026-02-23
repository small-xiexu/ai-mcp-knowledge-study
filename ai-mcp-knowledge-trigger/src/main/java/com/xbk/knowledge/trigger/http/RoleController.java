package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IRoleService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.role.RoleCreateRequest;
import com.xbk.knowledge.api.dto.role.RolePermissionGrantRequest;
import com.xbk.knowledge.api.dto.role.RolePermissionQueryRequest;
import com.xbk.knowledge.api.dto.role.RoleQueryRequest;
import com.xbk.knowledge.api.dto.role.RoleResponse;
import com.xbk.knowledge.api.dto.role.RoleUpdateRequest;
import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.RoleAppService;
import com.xbk.knowledge.domain.identity.model.entity.SysRole;
import com.xbk.knowledge.domain.identity.model.valobj.RolePageQuery;
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
 * 角色管理接口控制器。
 *
 * 职责：触发层接口适配，用于提供角色管理能力。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController implements IRoleService {

    private final RoleAppService roleAppService;
    private final AuthAppService authAppService;
    private final IdentityContextService identityContextService;

    /**
     * 分页查询角色。
     * 流程：
     * 1. 进入接口后执行 `role:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `RolePageQuery` 并调用 `roleAppService.queryRolePage`。
     * 4. 将领域分页结果转换为 `RoleResponse` 分页结果。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 查询参数
     * @return 分页结果
     */
    @SaCheckPermission("role:read")
    @PostMapping("/list")
    @Override
    public Result<PageResult<RoleResponse>> list(@Valid @RequestBody RoleQueryRequest request) {
        RolePageQuery query = new RolePageQuery(
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
     * 流程：
     * 1. 进入接口后执行 `role:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `SysRole` 领域对象并调用 `roleAppService.createRole`。
     * 4. 应用层完成角色唯一性校验与落库。
     * 5. 将创建结果转换为 `RoleResponse` 并统一返回。
     *
     * @param request 创建参数
     * @return 角色信息
     */
    @SaCheckPermission("role:write")
    @PostMapping("/create")
    @Override
    public Result<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        SysRole role = SysRole.builder()
                .roleCode(request.getRoleCode())
                .roleName(request.getRoleName())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
        SysRole saved = roleAppService.createRole(role);
        return Result.success("角色创建成功", toResponse(saved));
    }

    /**
     * 更新角色。
     * 流程：
     * 1. 进入接口后执行 `role:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装更新领域对象并调用 `roleAppService.updateRole`。
     * 4. 应用层执行角色状态与基础信息更新。
     * 5. 将更新结果转换为 `RoleResponse` 并统一返回。
     *
     * @param request 更新参数
     * @return 角色信息
     */
    @SaCheckPermission("role:write")
    @PostMapping("/update")
    @Override
    public Result<RoleResponse> update(@Valid @RequestBody RoleUpdateRequest request) {
        SysRole role = SysRole.builder()
                .id(request.getId())
                .roleName(request.getRoleName())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
        SysRole updated = roleAppService.updateRole(role);
        return Result.success("角色更新成功", toResponse(updated));
    }

    /**
     * 角色授权。
     * 流程：
     * 1. 进入接口后执行 `role:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 读取当前登录用户，作为授权操作人。
     * 4. 调用 `roleAppService.grantPermissions` 更新角色-权限关系。
     * 5. 统一封装空成功结果返回。
     *
     * @param request 授权请求
     * @return 响应
     */
    @SaCheckPermission("role:write")
    @PostMapping("/grant-permissions")
    @Override
    public Result<Void> grantPermissions(@Valid @RequestBody RolePermissionGrantRequest request) {
        AuthProfile currentProfile = currentProfile();
        Long operatorId = currentProfile.getUserId();
        roleAppService.grantPermissions(request.getRoleId(), request.getPermissionIds(), operatorId);
        return Result.success();
    }

    /**
     * 查询角色已绑定的权限 ID 列表。
     * 流程：
     * 1. 进入接口后执行 `role:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `roleAppService.queryPermissionIds` 查询绑定权限。
     * 4. 统一封装权限 ID 列表返回。
     *
     * @param request 查询参数
     * @return 权限ID列表
     */
    @SaCheckPermission("role:read")
    @PostMapping("/permission-ids")
    @Override
    public Result<List<Long>> permissionIds(@Valid @RequestBody RolePermissionQueryRequest request) {
        List<Long> permissionIds = roleAppService.queryPermissionIds(request.getRoleId());
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
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
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
        Long loginUserId = identityContextService.getCurrentUserId();
        return authAppService.loadProfile(loginUserId);
    }
}
