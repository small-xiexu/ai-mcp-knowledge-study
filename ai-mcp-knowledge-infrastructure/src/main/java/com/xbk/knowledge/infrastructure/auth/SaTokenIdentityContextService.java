package com.xbk.knowledge.infrastructure.auth;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import org.springframework.stereotype.Service;

/**
 * Sa-Token 身份上下文实现。
 *
 * 职责：基础设施适配，实现登录态与 token 读取能力。
 *
 * @author sxie
 */
@Service
public class SaTokenIdentityContextService implements IdentityContextService {

    /**
     * 执行用户登录并写入会话。
     *
     * @param userId 用户 ID
     */
    @Override
    public void login(Long userId) {
        StpUtil.login(userId);
    }

    /**
     * 执行用户登出并清理会话。
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID
     */
    @Override
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 判断当前请求是否已登录。
     *
     * @return 是否已登录
     */
    @Override
    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 获取 Token 名称。
     *
     * @return Token 名称
     */
    @Override
    public String getTokenName() {
        return getTokenInfo().getTokenName();
    }

    /**
     * 获取当前 Token 值。
     *
     * @return Token 值
     */
    @Override
    public String getTokenValue() {
        return getTokenInfo().getTokenValue();
    }

    /**
     * 获取当前 Token 过期剩余时间。
     *
     * @return 剩余秒数
     */
    @Override
    public long getTokenTimeout() {
        return getTokenInfo().getTokenTimeout();
    }

    private SaTokenInfo getTokenInfo() {
        return StpUtil.getTokenInfo();
    }
}
