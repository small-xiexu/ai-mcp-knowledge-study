package com.xbk.knowledge.types.enums;

/**
 * 任务类型枚举
 * 定义 AI 模型支持的任务类型
 *
 * 职责：通用枚举，用于统一业务语义
 * @author xiexu
 */
public enum TaskTypeEnum {

    /**
     * 分析任务（数据分析、逻辑推理等）
     */
    ANALYSIS("ANALYSIS", "分析"),

    /**
     * 写作任务（文章创作、内容生成等）
     */
    WRITING("WRITING", "写作"),

    /**
     * 翻译任务（多语言翻译）
     */
    TRANSLATION("TRANSLATION", "翻译"),

    /**
     * 代码生成任务（代码编写、调试等）
     */
    CODE_GENERATION("CODE_GENERATION", "代码生成"),

    /**
     * 对话任务（日常对话、问答等）
     */
    CONVERSATION("CONVERSATION", "对话"),

    /**
     * 总结任务（文本摘要、总结等）
     */
    SUMMARIZATION("SUMMARIZATION", "总结"),

    /**
     * MCP 协议对接任务
     */
    MCP_INTEGRATION("MCP_INTEGRATION", "对接MCP");

    /**
     * 任务类型校验正则
     * 用于接口层参数校验与枚举一致性约束
     */
    public static final String TASK_TYPE_REGEX =
            "^(ANALYSIS|WRITING|TRANSLATION|CODE_GENERATION|CONVERSATION|SUMMARIZATION|MCP_INTEGRATION)$";

    /**
     * 任务编码（与数据库 task_code 字段对应）
     */
    private final String code;

    /**
     * 任务显示名称
     */
    private final String displayName;

    TaskTypeEnum(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 对外暴露 getCode 作为调用入口，便于上层复用。
     */
    public String getCode() {
        return code;
    }

    /**
     * 对外暴露 getDisplayName 作为调用入口，便于上层复用。
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据任务编码获取枚举
     *
     * @param code 任务编码
     * @return 任务类型枚举
     */
    public static TaskTypeEnum fromCode(String code) {
        for (TaskTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的任务类型编码: " + code);
    }
}
