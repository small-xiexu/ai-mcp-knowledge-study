package com.xbk.knowledge.types.enums;

/**
 * 调用状态枚举
 * 记录 AI 模型调用的结果状态
 *
 * @author xiexu
 */
public enum CallStatus {

    /**
     * 调用成功
     */
    SUCCESS("成功"),

    /**
     * 调用失败
     */
    FAILED("失败"),

    /**
     * 使用降级模型成功
     */
    FALLBACK("降级成功");

    /**
     * 状态显示名称
     */
    private final String displayName;

    CallStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
