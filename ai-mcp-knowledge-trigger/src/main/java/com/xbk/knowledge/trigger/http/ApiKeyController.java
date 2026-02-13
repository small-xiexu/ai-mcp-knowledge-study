package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xbk.knowledge.api.dto.apikey.ApiKeyCreateRequest;
import com.xbk.knowledge.api.dto.apikey.ApiKeyCreateResponse;
import com.xbk.knowledge.api.dto.apikey.ApiKeyQueryRequest;
import com.xbk.knowledge.api.dto.apikey.ApiKeyResponse;
import com.xbk.knowledge.api.dto.apikey.ApiKeyRevokeRequest;
import com.xbk.knowledge.application.model.identity.ApiKeyCreateResult;
import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.application.service.app.ApiKeyAppService;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.domain.model.entity.SysApiKey;
import com.xbk.knowledge.domain.model.vo.identity.ApiKeyPageQuery;
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

/**
 * API Key 管理接口控制器。
 *
 * 职责：触发层接口适配，用于提供服务账号密钥管理能力。
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/apikeys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyAppService apiKeyAppService;
    private final AuthAppService authAppService;

    /**
     * 分页查询 API Key。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @SaCheckPermission("user:read")
    @PostMapping("/list")
    public Result<PageResult<ApiKeyResponse>> list(@Valid @RequestBody ApiKeyQueryRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        ApiKeyPageQuery query = new ApiKeyPageQuery(
                tenantId,
                request.getOwnerUserId(),
                request.getStatus(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<SysApiKey> page = apiKeyAppService.queryPage(query);
        PageResult<ApiKeyResponse> responsePage = PageResultConverter.convert(page, this::toResponse);
        return Result.success(responsePage);
    }

    /**
     * 创建 API Key。
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @SaCheckPermission("user:write")
    @PostMapping("/create")
    public Result<ApiKeyCreateResponse> create(@RequestBody(required = false) ApiKeyCreateRequest request) {
        ApiKeyCreateRequest safeRequest = request == null ? ApiKeyCreateRequest.builder().build() : request;
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, safeRequest.getTenantId());
        Long ownerUserId = safeRequest.getOwnerUserId() == null ? currentProfile.getUserId() : safeRequest.getOwnerUserId();
        ApiKeyCreateResult createResult = apiKeyAppService.create(
                tenantId,
                ownerUserId,
                safeRequest.getScopes(),
                safeRequest.getExpireAt()
        );
        ApiKeyCreateResponse response = toCreateResponse(createResult);
        return Result.success("API Key 创建成功", response);
    }

    /**
     * 禁用 API Key。
     *
     * @param request 禁用请求
     * @return 响应
     */
    @SaCheckPermission("user:write")
    @PostMapping("/revoke")
    public Result<Void> revoke(@Valid @RequestBody ApiKeyRevokeRequest request) {
        AuthProfile currentProfile = currentProfile();
        String tenantId = resolveTenantId(currentProfile, request.getTenantId());
        apiKeyAppService.revoke(tenantId, request.getId());
        return Result.success();
    }

    /**
     * 转换 API Key 响应。
     *
     * @param apiKey 实体
     * @return 响应 DTO
     */
    private ApiKeyResponse toResponse(SysApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .tenantId(apiKey.getTenantId())
                .ownerUserId(apiKey.getOwnerUserId())
                .accessKey(apiKey.getAccessKey())
                .scopes(apiKey.getScopes())
                .status(apiKey.getStatus())
                .expireAt(apiKey.getExpireAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .build();
    }

    /**
     * 转换创建响应。
     *
     * @param createResult 创建结果
     * @return 响应 DTO
     */
    private ApiKeyCreateResponse toCreateResponse(ApiKeyCreateResult createResult) {
        SysApiKey apiKey = createResult.getApiKey();
        return ApiKeyCreateResponse.builder()
                .id(apiKey.getId())
                .tenantId(apiKey.getTenantId())
                .ownerUserId(apiKey.getOwnerUserId())
                .accessKey(apiKey.getAccessKey())
                .scopes(apiKey.getScopes())
                .status(apiKey.getStatus())
                .expireAt(apiKey.getExpireAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .secret(createResult.getPlainSecret())
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
