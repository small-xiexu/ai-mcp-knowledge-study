package com.xbk.knowledge.types.enums;

/**
 * 网关状态枚举
 *
 * 职责：定义网关实例/凭证/工具的启用/禁用状态
 * @author sxie
 */
public enum GatewayStatus {
    /**
     * 启用
     */
     ENABLED(1, "启用"),
    /**
     * 禁用
     */
     DISABLED(0, "禁用");

    private final int code;
    private final String desc;

    GatewayStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据状态码获取枚举
     * @param code 状态码（0 或 1）
     * @return 对应的枚举值
     * @throws IllegalArgumentException 未知状态码时抛出
     */
    public static GatewayStatus fromCode(int code) {
        for (GatewayStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知状态码：" + code);
    }
}
