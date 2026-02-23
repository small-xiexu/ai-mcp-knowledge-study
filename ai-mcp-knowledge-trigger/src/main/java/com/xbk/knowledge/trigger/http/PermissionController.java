package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IPermissionService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.permission.PermissionQueryRequest;
import com.xbk.knowledge.api.dto.permission.PermissionResponse;
import com.xbk.knowledge.application.service.app.PermissionAppService;
import com.xbk.knowledge.domain.identity.model.entity.SysPermission;
import com.xbk.knowledge.domain.identity.model.valobj.PermissionPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 权限查询接口控制器。
 *
 * 职责：触发层接口适配，用于提供权限读取能力。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController implements IPermissionService {

    private final PermissionAppService permissionAppService;

    /**
     * 分页查询权限。
     * 流程：
     * 1. 进入 Trigger 层后执行 `role:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `PermissionPageQuery` 并调用 `permissionAppService.queryPermissionPage`。
     * 4. 将领域对象分页结果转换为 `PermissionResponse` 分页结果。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 查询参数
     * @return 分页结果
     */
    @SaCheckPermission("role:read")
    @PostMapping("/list")
    @Override
    public Result<PageResult<PermissionResponse>> list(@Valid @RequestBody PermissionQueryRequest request) {
        PermissionPageQuery query = new PermissionPageQuery(
                request.getResourceType(),
                request.getAction(),
                request.getStatus(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<SysPermission> permissionPage = permissionAppService.queryPermissionPage(query);
        PageResult<PermissionResponse> responsePage = PageResultConverter.convert(permissionPage, this::toResponse);
        return Result.success(responsePage);
    }

    /**
     * 转换权限响应。
     *
     * @param permission 权限实体
     * @return 响应 DTO
     */
    private PermissionResponse toResponse(SysPermission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .permissionCode(permission.getPermissionCode())
                .permissionName(permission.getPermissionName())
                .resourceType(permission.getResourceType())
                .action(permission.getAction())
                .status(permission.getStatus())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}
