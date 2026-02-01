package com.xbk.knowledge.trigger.security;

import org.springframework.stereotype.Component;

/**
 * XXL 权限守卫
 * 预留最小权限校验入口，便于后续接入认证与授权
 *
 * @author xiexu
 */
@Component
public class XxlPermissionGuard {

    /**
     * 任务查看权限校验
     *
     * 为什么：接口层先校验权限，避免非法请求进入下游调用链。
     */
    public void assertCanView() {
        // 最小权限占位，后续接入认证后替换
    }

    /**
     * 任务编辑权限校验
     *
     * 为什么：编辑类操作风险更高，需要明确权限隔离。
     */
    public void assertCanEdit() {
        // 最小权限占位，后续接入认证后替换
    }
}
