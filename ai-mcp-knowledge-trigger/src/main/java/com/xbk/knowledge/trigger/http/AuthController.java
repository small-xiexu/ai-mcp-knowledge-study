package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xbk.knowledge.api.dto.auth.AuthLoginRequest;
import com.xbk.knowledge.api.dto.auth.AuthLoginResponse;
import com.xbk.knowledge.api.dto.auth.AuthProfileResponse;
import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.domain.identity.model.entity.SysUser;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 认证接口控制器。
 *
 * 职责：触发层接口适配，用于提供登录态相关能力。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;
    private final IdentityContextService identityContextService;

    /**
     * 登录接口。
     *
     * @param request     登录请求
     * @param httpRequest HTTP请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public Result<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request, HttpServletRequest httpRequest) {
        SysUser user = authAppService.verifyLogin(request.getUsername(), request.getPassword());
        identityContextService.login(user.getId());
        String loginIp = resolveClientIp(httpRequest);
        authAppService.recordLoginSuccess(user.getId(), loginIp);
        AuthProfile profile = authAppService.loadProfile(user.getId());
        AuthLoginResponse response = AuthLoginResponse.builder()
                .tokenName(identityContextService.getTokenName())
                .tokenValue(identityContextService.getTokenValue())
                .tokenTimeout(identityContextService.getTokenTimeout())
                .profile(toProfileResponse(profile))
                .build();
        return Result.success("登录成功", response);
    }

    /**
     * 登出接口。
     *
     * @return 响应
     */
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        identityContextService.logout();
        return Result.success();
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前登录用户画像
     */
    @SaCheckLogin
    @GetMapping("/me")
    public Result<AuthProfileResponse> currentUser() {
        Long userId = identityContextService.getCurrentUserId();
        AuthProfile profile = authAppService.loadProfile(userId);
        return Result.success(toProfileResponse(profile));
    }

    /**
     * 转换为响应模型。
     *
     * @param profile 应用层模型
     * @return 响应 DTO
     */
    private AuthProfileResponse toProfileResponse(AuthProfile profile) {
        return AuthProfileResponse.builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .email(profile.getEmail())
                .mobile(profile.getMobile())
                .superAdmin(profile.getSuperAdmin())
                .roles(profile.getRoleCodes())
                .permissions(profile.getPermissionCodes())
                .build();
    }

    /**
     * 解析客户端IP。
     *
     * @param request HTTP 请求
     * @return IP
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
