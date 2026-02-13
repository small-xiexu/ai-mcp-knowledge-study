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
 * @author xiexu
 */
@Service
public class SaTokenIdentityContextService implements IdentityContextService {

    @Override
    public void login(Long userId) {
        StpUtil.login(userId);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    @Override
    public String getTokenName() {
        return getTokenInfo().getTokenName();
    }

    @Override
    public String getTokenValue() {
        return getTokenInfo().getTokenValue();
    }

    @Override
    public long getTokenTimeout() {
        return getTokenInfo().getTokenTimeout();
    }

    private SaTokenInfo getTokenInfo() {
        return StpUtil.getTokenInfo();
    }
}
