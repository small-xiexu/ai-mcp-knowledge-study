package com.xbk.knowledge.types.context;

/**
 * 组织上下文持有者（ThreadLocal）。
 *
 * 职责：
 * - 在请求入口（HTTP Filter / Job Aspect）写入 OrgContext
 * - 在应用层/基础设施层读取 currentOrgId，用于资源归属
 *
 * @author xiexu
 */
public final class OrgContextHolder {

    public static final long SINGLE_ORG_ID = 1L;

    private static final ThreadLocal<OrgContext> CONTEXT = new ThreadLocal<>();

    private OrgContextHolder() {
    }

    /**
     * set。
     *
     * @param context 参数
     */
    public static void set(OrgContext context) {
        CONTEXT.set(context);
    }

    /**
     * get。
     *
     * @return 返回结果
     */
    public static OrgContext get() {
        return CONTEXT.get();
    }

    /**
     * clear。
     *
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前请求的目标组织ID（资源归属）。
     * 若未注入上下文则回退为默认组织ID（1）。
     */
    public static Long currentOrgIdOrNull() {
        OrgContext ctx = CONTEXT.get();
        return ctx == null ? SINGLE_ORG_ID : ctx.currentOrgId();
    }

    /**
     * 获取操作者所属组织ID（责任归属）。
     */
    public static Long operatorOrgIdOrNull() {
        OrgContext ctx = CONTEXT.get();
        return ctx == null ? SINGLE_ORG_ID : ctx.operatorOrgId();
    }

}
