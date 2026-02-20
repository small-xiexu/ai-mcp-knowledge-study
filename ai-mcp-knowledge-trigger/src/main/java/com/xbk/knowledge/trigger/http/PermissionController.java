package com.xbk.knowledge.trigger.http;

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
 * @author xiexu
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionAppService permissionAppService;

    /**
     * 分页查询权限。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @SaCheckPermission("role:read")
    @PostMapping("/list")
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
