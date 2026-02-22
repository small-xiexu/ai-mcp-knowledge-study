package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.auth.AuthLoginRequest;
import com.xbk.knowledge.api.dto.auth.AuthLoginResponse;
import com.xbk.knowledge.api.dto.auth.AuthProfileResponse;
import com.xbk.knowledge.types.common.Result;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证服务接口
 * 定义登录态与用户认证的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IAuthService {

    /**
     * 执行登录。
     *
     * @param request 请求参数
     * @param httpRequest HTTP 请求对象
     * @return 登录结果
     */
    Result<AuthLoginResponse> login(AuthLoginRequest request, HttpServletRequest httpRequest);

    /**
     * 执行登出。
     *
     * @return 处理结果
     */
    Result<Void> logout();

    /**
     * 查询当前登录用户。
     *
     * @return 查询结果
     */
    Result<AuthProfileResponse> currentUser();
}
