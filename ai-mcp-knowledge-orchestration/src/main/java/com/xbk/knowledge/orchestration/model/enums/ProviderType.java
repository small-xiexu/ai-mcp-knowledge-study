package com.xbk.knowledge.orchestration.model.enums;

/**
 * AI 模型 API 提供商类型枚举
 *
 * <p>定义支持的 AI 模型 API 协议类型，用于标识使用哪个提供商的 API 接口。
 *
 * <p><b>重要说明</b>：
 * <ul>
 *   <li>此枚举表示的是 <b>API 提供商/协议类型</b>，而非具体的模型名称</li>
 *   <li>同一个提供商类型可以对接多个不同的模型</li>
 *   <li>例如：OPENAI 类型可以对接 GPT-4、GPT-3.5、DeepSeek、智谱等 OpenAI 兼容模型</li>
 * </ul>
 *
 * <p><b>使用示例</b>：
 * <pre>{@code
 * ModelConfig config = new ModelConfig();
 * config.setProviderType(ProviderType.OPENAI);  // 使用 OpenAI API 协议
 * config.setModelName("gpt-4");                 // 具体的模型名称
 * config.setBaseUrl("https://api.openai.com"); // API 端点
 * }</pre>
 *
 * <p><b>扩展性</b>：
 * <ul>
 *   <li>OPENAI 类型支持所有 OpenAI 兼容的 API（官方 OpenAI、DeepSeek、智谱、Moonshot 等）</li>
 *   <li>通过 baseUrl 和 modelName 配置，可以灵活对接不同的服务提供商</li>
 * </ul>
 *
 * @author xiexu
 * @since 2026-01-28
 */
public enum ProviderType {

    /**
     * OpenAI API 协议
     *
     * <p>支持的模型示例：
     * <ul>
     *   <li>OpenAI 官方：gpt-4, gpt-4-turbo, gpt-3.5-turbo</li>
     *   <li>DeepSeek：deepseek-chat, deepseek-coder</li>
     *   <li>智谱 AI：glm-4, glm-3-turbo</li>
     *   <li>Moonshot：moonshot-v1-8k, moonshot-v1-32k</li>
     *   <li>其他 OpenAI 兼容的第三方 API</li>
     * </ul>
     *
     * <p>API 特点：
     * <ul>
     *   <li>使用 OpenAI 标准的 Chat Completions API</li>
     *   <li>支持 Function Calling（工具调用）</li>
     *   <li>支持流式响应（Streaming）</li>
     * </ul>
     */
    OPENAI("OpenAI API"),

    /**
     * Anthropic API 协议
     *
     * <p>支持的模型示例：
     * <ul>
     *   <li>Claude 3.5 Sonnet</li>
     *   <li>Claude 3 Opus</li>
     *   <li>Claude 3 Haiku</li>
     * </ul>
     *
     * <p>API 特点：
     * <ul>
     *   <li>使用 Anthropic Messages API</li>
     *   <li>支持 Tool Use（工具调用）</li>
     *   <li>支持流式响应</li>
     *   <li>上下文窗口大（最高 200K tokens）</li>
     * </ul>
     */
    ANTHROPIC("Anthropic API"),

    /**
     * Google Gemini API 协议
     *
     * <p>支持的模型示例：
     * <ul>
     *   <li>Gemini 3 Flash</li>
     *   <li>Gemini 2.0 Flash</li>
     *   <li>Gemini 1.5 Pro</li>
     * </ul>
     *
     * <p>API 特点：
     * <ul>
     *   <li>使用 Google GenAI API</li>
     *   <li>支持 Function Calling</li>
     *   <li>支持多模态（文本、图像、视频）</li>
     *   <li>免费额度较高</li>
     * </ul>
     */
    GEMINI("Google Gemini API");

    /**
     * 提供商类型的显示名称
     * 用于 UI 展示和日志输出
     */
    private final String displayName;

    /**
     * 构造函数
     *
     * @param displayName 显示名称
     */
    ProviderType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取显示名称
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
}
