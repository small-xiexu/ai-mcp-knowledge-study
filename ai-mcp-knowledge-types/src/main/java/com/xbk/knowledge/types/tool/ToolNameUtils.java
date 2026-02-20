package com.xbk.knowledge.types.tool;

/**
 * 工具命名工具类。
 *
 * 目的：
 * - 为 LLM function/tool name 提供稳定、可预测、尽量兼容各厂商约束的命名格式。
 * - 避免不同来源（Gateway/MCP）与不同实例（gatewayId/serverName）之间的重名冲突。
 *
 * @author sxie
 */
public final class ToolNameUtils {

    private static final int MAX_NAME_LEN = 64;

    private ToolNameUtils() {
    }

    /**
     * 生成安全的工具函数名。
     *
     * 输出格式：
     * - {prefix}_{namespaceId}__{toolName}
     *
     * 规则：
     * - 仅保留 [A-Za-z0-9_-]，其它字符替换为下划线
     * - 超长截断到 64
     */
    public static String safeFunctionName(String prefix, String namespaceId, String toolName) {
        String p = sanitize(prefix == null ? "" : prefix);
        if (p.isBlank()) {
            p = "tool";
        }
        String ns = sanitize(namespaceId == null ? "" : namespaceId);
        String tn = sanitize(toolName == null ? "" : toolName);
        if (ns.isBlank()) {
            ns = "default";
        }
        if (tn.isBlank()) {
            tn = "unknown";
        }
        String name = p + "_" + ns + "__" + tn;
        if (name.length() <= MAX_NAME_LEN) {
            return name;
        }
        return name.substring(0, MAX_NAME_LEN);
    }

    private static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_'
                    || c == '-';
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }
}

