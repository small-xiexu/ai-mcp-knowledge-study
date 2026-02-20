package com.xbk.knowledge.types.tool;

/**
 * 工具调用权限绕过上下文（ThreadLocal）。
 *
 * 职责：用于“方式B”审批通过后平台代执行工具的场景，允许在保留审计与 runId 贯穿的前提下，
 * 临时绕过 tool:invoke 的 RBAC 校验。
 *
 * 约束：仅在极少数平台内部流程中使用，必须成对 set/clear，避免线程复用污染。
 *
 * @author sxie
 */
public final class ToolInvokeBypassContextHolder {

    private static final ThreadLocal<Boolean> BYPASS = new ThreadLocal<>();

    private ToolInvokeBypassContextHolder() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * enable。
     *
     */
    public static void enable() {
        BYPASS.set(Boolean.TRUE);
    }

    /**
     * clear。
     *
     */
    public static void clear() {
        BYPASS.remove();
    }

    /**
     * isEnabled。
     *
     * @return 返回结果
     */
    public static boolean isEnabled() {
        Boolean v = BYPASS.get();
        return Boolean.TRUE.equals(v);
    }
}

