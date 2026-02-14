package com.xbk.knowledge.types.context;

import com.xbk.knowledge.types.exception.BusinessException;

/**
 * 组织上下文持有者（ThreadLocal）。
 *
 * 职责：
 * - 在请求入口（HTTP Filter / Job Aspect）写入 OrgContext
 * - 在应用层/基础设施层读取 currentOrgId，用于 org 隔离过滤与写入归属
 */
public final class OrgContextHolder {

    private static final ThreadLocal<OrgContext> CONTEXT = new ThreadLocal<>();

    private OrgContextHolder() {
    }

    public static void set(OrgContext context) {
        CONTEXT.set(context);
    }

    public static OrgContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前请求的目标组织ID（资源归属）。
     * 若未注入上下文则返回 null，由上层决定兜底策略（例如 ROOT org）。
     */
    public static Long currentOrgIdOrNull() {
        OrgContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.currentOrgId();
    }

    /**
     * 获取操作者所属组织ID（责任归属）。
     */
    public static Long operatorOrgIdOrNull() {
        OrgContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.operatorOrgId();
    }

    /**
     * 超级管理员写操作前置校验：必须显式选择 target org。
     */
    public static void requireExplicitTargetOrgIfSuperAdmin() {
        OrgContext ctx = CONTEXT.get();
        if (ctx == null) {
            return;
        }
        if (ctx.superAdmin() && !ctx.explicitTargetOrg()) {
            throw new BusinessException("超级管理员请先显式选择目标组织（targetOrgId）再进行写操作");
        }
    }
}

