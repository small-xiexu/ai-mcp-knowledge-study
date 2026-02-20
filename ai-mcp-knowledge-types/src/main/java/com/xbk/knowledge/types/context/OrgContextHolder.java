package com.xbk.knowledge.types.context;

/**
 * 兼容占位：项目已切换为单组织模式，固定返回组织 ID=1。
 * @author sxie
 */
public final class OrgContextHolder {

    private OrgContextHolder() {
    }

    public static Long currentOrgIdOrNull() {
        return 1L;
    }
}
