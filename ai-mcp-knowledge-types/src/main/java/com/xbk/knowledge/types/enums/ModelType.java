package com.xbk.knowledge.types.enums;

/**
 * 模型类型枚举
 * 定义支持的 AI 模型提供商类型
 *
 * 职责：通用枚举，用于统一业务语义
 * @author sxie
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
    GEMINI("Gemini"),

    /**
     * Ollama 模型（本地/私有化大模型）
     */
    OLLAMA("Ollama"),

    /**
     * DeepSeek 模型（OpenAI 兼容接口）
     */
    DEEPSEEK("DeepSeek");

    /**
     * 模型类型显示名称
     */
    private final String displayName;

    ModelType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 对外暴露 getDisplayName 作为调用入口，便于上层复用。
     */
    public String getDisplayName() {
        return displayName;
    }
}
