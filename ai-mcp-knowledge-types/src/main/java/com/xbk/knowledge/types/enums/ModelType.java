package com.xbk.knowledge.types.enums;

/**
 * 模型类型枚举
 * 定义支持的 AI 模型提供商类型
 *
 * @author xiexu
 */
public enum ModelType {

    /**
     * OpenAI 模型（GPT-4、GPT-3.5 等）
     */
    OPENAI("OpenAI"),

    /**
     * Anthropic 模型（Claude 3.5 Sonnet 等）
     */
    ANTHROPIC("Anthropic"),

    /**
     * Google Gemini 模型（Gemini 3 Flash 等）
     */
    GEMINI("Gemini");

    /**
     * 模型类型显示名称
     */
    private final String displayName;

    ModelType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
