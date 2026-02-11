package com.xbk.knowledge.test;

import com.xbk.knowledge.Application;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.application.service.runtime.McpServerRuntimeService;
import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.types.enums.ModelType;
import com.xbk.knowledge.types.enums.McpServerType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MCP Gateway SSE + tools/list 集成测试
 * 目标：验证 MCP 客户端可以通过 SSE 建立会话并拉取工具清单
 *
 * 说明：
 * 本测试包含两部分：
 * 1. 验证 SSE + tools/list
 * 2. 通过 OpenAI 模型验证 tools/call 全链路（包含真实工具调用）
 *
 * 前置条件：
 * 1. ai-mcp-gateway-study 服务已启动（默认 http://localhost:8091）
 * 2. gatewayId 对应的工具已配置并可用（默认 gateway_001）
 * 可通过系统属性或环境变量覆盖默认值：
 * - baseUrl: mcp.gateway.base-url / MCP_GATEWAY_BASE_URL
 * - contextPath: mcp.gateway.context-path / MCP_GATEWAY_CONTEXT_PATH
 * - gatewayId: mcp.gateway.gateway-id / MCP_GATEWAY_ID
 * - sseEndpoint: mcp.gateway.sse-endpoint / MCP_GATEWAY_SSE_ENDPOINT
 * - toolName: mcp.gateway.tool-name / MCP_GATEWAY_TOOL_NAME
 * - modelId: mcp.gateway.model-id / MCP_GATEWAY_MODEL_ID
 * - modelPrompt: mcp.gateway.model-prompt / MCP_GATEWAY_MODEL_PROMPT
 *
 * @author xiexu
 */
@Slf4j
@Tag("integration")
@SpringBootTest(classes = Application.class)
@ImportAutoConfiguration(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class McpGatewaySseToolsListTest {

    /**
     * MCP 服务运行时注册器：用于动态注册/注销 SSE MCP Server。
     */
    private final McpServerRuntimeService runtimeService;

    /**
     * 工具回调提供器：用于读取 tools/list 返回的工具定义。
     */
    private final ToolCallbackProvider toolCallbackProvider;

    /**
     * ChatClient 装配器：用于构建带 MCP 工具增强的模型客户端。
     */
    private final ChatClientAssemblyService chatClientAssemblyService;

    /**
     * 模型配置应用服务：用于查询可用 OpenAI 模型。
     */
    private final ModelConfigAppService modelConfigAppService;

    /**
     * 构造注入测试依赖。
     */
    @Autowired
    public McpGatewaySseToolsListTest(McpServerRuntimeService runtimeService,
                                      ToolCallbackProvider toolCallbackProvider,
                                      ChatClientAssemblyService chatClientAssemblyService,
                                      ModelConfigAppService modelConfigAppService) {
        this.runtimeService = runtimeService;
        this.toolCallbackProvider = toolCallbackProvider;
        this.chatClientAssemblyService = chatClientAssemblyService;
        this.modelConfigAppService = modelConfigAppService;
    }

    /**
     * 仅验证网关 SSE + tools/list。
     *
     * 验证点：
     * 1. 运行时可注册 SSE MCP Server
     * 2. ToolCallbackProvider 能拿到工具列表
     * 3. 工具列表中包含指定工具名
     */
    @Test
    public void test_sse_tools_list_from_gateway() {
        String baseUrl = getConfig("mcp.gateway.base-url", "MCP_GATEWAY_BASE_URL", "http://localhost:8091");
        String contextPath = getConfig("mcp.gateway.context-path", "MCP_GATEWAY_CONTEXT_PATH", "/api-gateway");
        String gatewayId = getConfig("mcp.gateway.gateway-id", "MCP_GATEWAY_ID", "gateway_001");
        String defaultSseEndpoint = buildSseEndpoint(contextPath, gatewayId);
        String sseEndpoint = getConfig("mcp.gateway.sse-endpoint", "MCP_GATEWAY_SSE_ENDPOINT", defaultSseEndpoint);
        String expectedToolName = getConfig("mcp.gateway.tool-name", "MCP_GATEWAY_TOOL_NAME", "sendWeixinNotice");
        Integer connectTimeoutMs = getIntConfig("mcp.gateway.connect-timeout-ms", "MCP_GATEWAY_CONNECT_TIMEOUT_MS", 10000);
        Integer requestTimeoutMs = getIntConfig("mcp.gateway.request-timeout-ms", "MCP_GATEWAY_REQUEST_TIMEOUT_MS", 120000);
        Integer initTimeoutMs = getIntConfig("mcp.gateway.init-timeout-ms", "MCP_GATEWAY_INIT_TIMEOUT_MS", 300000);

        String endpoint = trimTrailingSlash(baseUrl);
        Long configId = System.currentTimeMillis();

        log.info("MCP SSE 测试配置 baseUrl: {}, sseEndpoint: {}, gatewayId: {}", endpoint, sseEndpoint, gatewayId);
        log.info("MCP SSE 超时配置 connect: {}ms, request: {}ms, init: {}ms",
                connectTimeoutMs, requestTimeoutMs, initTimeoutMs);

        McpServerConfig config = McpServerConfig.builder()
                .id(configId)
                .serverName("mcp-gateway-sse-" + gatewayId)
                .serverType(McpServerType.SSE)
                .enabled(true)
                .endpoint(endpoint)
                .sseEndpoint(sseEndpoint)
                .connectTimeoutMs(connectTimeoutMs)
                .requestTimeoutMs(requestTimeoutMs)
                .initTimeoutMs(initTimeoutMs)
                .build();

        try {
            // 注册后会触发 initialize + tools/list
            runtimeService.registerOrUpdate(config);

            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            assertNotNull(callbacks, "❌ ToolCallbackProvider 未返回工具");
            assertTrue(callbacks.length > 0, "❌ tools/list 未返回任何工具");
            String toolNames = collectToolNames(callbacks);
            log.info("tools/list 工具清单: {}", toolNames);

            boolean matched = hasTool(callbacks, expectedToolName);
            assertTrue(matched, "❌ 未找到期望工具: " + expectedToolName + "，实际工具: " + toolNames);

            log.info("✅ tools/list 校验成功，工具数量: {}", callbacks.length);
        } finally {
            // 释放连接，避免测试污染后续运行环境
            runtimeService.unregister(configId);
        }
    }

    /**
     * 通过大模型验证 SSE + tools/list + tools/call 全链路。
     *
     * 核心思路：
     * 1. 先完成 SSE 注册并断言 tools/list 返回目标工具
     * 2. 再发起一次 OpenAI 模型调用，要求必须调用工具
     * 3. 断言模型返回有效文本，且不出现“工具不可用/未找到”等失败语义
     */
    @Test
    public void test_sse_tools_list_with_openai_model_tool_call() {
        String baseUrl = getConfig("mcp.gateway.base-url", "MCP_GATEWAY_BASE_URL", "http://localhost:8091");
        String contextPath = getConfig("mcp.gateway.context-path", "MCP_GATEWAY_CONTEXT_PATH", "/api-gateway");
        String gatewayId = getConfig("mcp.gateway.gateway-id", "MCP_GATEWAY_ID", "gateway_001");
        String defaultSseEndpoint = buildSseEndpoint(contextPath, gatewayId);
        String sseEndpoint = getConfig("mcp.gateway.sse-endpoint", "MCP_GATEWAY_SSE_ENDPOINT", defaultSseEndpoint);
        String expectedToolName = getConfig("mcp.gateway.tool-name", "MCP_GATEWAY_TOOL_NAME", "sendWeixinNotice");
        String modelPrompt = getConfig(
                "mcp.gateway.model-prompt",
                "MCP_GATEWAY_MODEL_PROMPT",
                "请务必调用工具 sendWeixinNotice，参数：platform=测试平台，subject=网关联调验证，description=SSE+tools/list+tools/call 集成测试，jumpUrl=https://example.com。调用完成后用中文简短回复结果。"
        );
        Long preferredModelId = getLongConfig("mcp.gateway.model-id", "MCP_GATEWAY_MODEL_ID", null);

        Integer connectTimeoutMs = getIntConfig("mcp.gateway.connect-timeout-ms", "MCP_GATEWAY_CONNECT_TIMEOUT_MS", 10000);
        Integer requestTimeoutMs = getIntConfig("mcp.gateway.request-timeout-ms", "MCP_GATEWAY_REQUEST_TIMEOUT_MS", 120000);
        Integer initTimeoutMs = getIntConfig("mcp.gateway.init-timeout-ms", "MCP_GATEWAY_INIT_TIMEOUT_MS", 300000);

        String endpoint = trimTrailingSlash(baseUrl);
        Long configId = System.currentTimeMillis();

        log.info("MCP 模型联调(启用 tools/call)配置 baseUrl: {}, sseEndpoint: {}, gatewayId: {}",
                endpoint, sseEndpoint, gatewayId);

        McpServerConfig config = McpServerConfig.builder()
                .id(configId)
                .serverName("mcp-gateway-sse-openai-call-" + gatewayId)
                .serverType(McpServerType.SSE)
                .enabled(true)
                .endpoint(endpoint)
                .sseEndpoint(sseEndpoint)
                .connectTimeoutMs(connectTimeoutMs)
                .requestTimeoutMs(requestTimeoutMs)
                .initTimeoutMs(initTimeoutMs)
                .build();

        try {
            runtimeService.registerOrUpdate(config);

            // 先验证 tools/list（这是当前要验证的核心）
            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            assertNotNull(callbacks, "❌ ToolCallbackProvider 未返回工具");
            assertTrue(callbacks.length > 0, "❌ tools/list 未返回任何工具");
            String toolNames = collectToolNames(callbacks);
            log.info("tools/list 工具清单: {}", toolNames);
            assertTrue(hasTool(callbacks, expectedToolName),
                    "❌ 未找到期望工具: " + expectedToolName + "，实际工具: " + toolNames);

            // 再走模型链路，显式要求工具调用，验证 tools/call 真实执行
            ModelConfig openAiModel = resolveOpenAiModelConfig(preferredModelId);
            assertNotNull(openAiModel, "❌ 未找到可用的 OpenAI 模型配置");

            OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                    .model(openAiModel.getModelName())
                    .toolChoice("required")
                    .internalToolExecutionEnabled(Boolean.TRUE)
                    .build();
            ChatClient chatClient = chatClientAssemblyService.buildChatClient(openAiModel);
            ChatResponse response = chatClient.prompt()
                    .toolNames(expectedToolName)
                    .user(modelPrompt)
                    .options(openAiChatOptions)
                    .call()
                    .chatResponse();

            assertNotNull(response, "❌ 模型调用未返回响应");
            String assistantContent = extractAssistantContent(response);
            assertTrue(assistantContent != null && !assistantContent.isBlank(), "❌ 模型返回内容为空");
            assertTrue(!containsToolFailureKeyword(assistantContent),
                    "❌ 模型返回工具调用失败语义，content: " + assistantContent);
            log.info("✅ 模型链路验证通过（tools/list + tools/call），model: {}",
                    openAiModel.getModelName());
            log.info("模型最终回复: {}", assistantContent);
        } finally {
            runtimeService.unregister(configId);
        }
    }

    /**
     * 解析要使用的 OpenAI 模型。
     *
     * 优先级：
     * 1. 显式指定 modelId
     * 2. 当前激活且启用的 OpenAI 对话模型
     * 3. 启用列表中的任一 OpenAI 模型
     */
    private ModelConfig resolveOpenAiModelConfig(Long preferredModelId) {
        if (preferredModelId != null) {
            ModelConfig byId = modelConfigAppService.queryModelConfigById(new IdQuery(preferredModelId));
            assertNotNull(byId, "❌ 指定模型ID不存在: " + preferredModelId);
            assertTrue(ModelType.OPENAI == byId.getModelType(), "❌ 指定模型不是 OPENAI: " + preferredModelId);
            assertTrue(Boolean.TRUE.equals(byId.getEnabled()), "❌ 指定模型未启用: " + preferredModelId);
            return byId;
        }

        ModelConfig activeChatModel = modelConfigAppService.getActiveChatModel();
        if (activeChatModel != null
                && ModelType.OPENAI == activeChatModel.getModelType()
                && Boolean.TRUE.equals(activeChatModel.getEnabled())) {
            return activeChatModel;
        }

        List<ModelConfig> enabledModels = modelConfigAppService.queryEnabledModels(new EnabledQuery(true));
        if (enabledModels == null || enabledModels.isEmpty()) {
            return null;
        }
        for (ModelConfig modelConfig : enabledModels) {
            if (modelConfig == null) {
                continue;
            }
            if (ModelType.OPENAI == modelConfig.getModelType() && Boolean.TRUE.equals(modelConfig.getEnabled())) {
                return modelConfig;
            }
        }
        return null;
    }

    /**
     * 判断工具列表中是否存在指定工具。
     */
    private boolean hasTool(ToolCallback[] callbacks, String expectedName) {
        if (callbacks == null || callbacks.length == 0 || expectedName == null) {
            return false;
        }
        for (ToolCallback callback : callbacks) {
            if (callback == null) {
                continue;
            }
            String name = callback.getToolDefinition().name();
            if (expectedName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 收集 tools/list 的工具名称，便于日志与断言排查。
     */
    private String collectToolNames(ToolCallback[] callbacks) {
        if (callbacks == null || callbacks.length == 0) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (ToolCallback callback : callbacks) {
            if (callback == null || callback.getToolDefinition() == null) {
                continue;
            }
            String name = callback.getToolDefinition().name();
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (!first) {
                builder.append(", ");
            }
            builder.append(name);
            first = false;
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 提取模型最终文本输出，便于断言与日志排查。
     */
    private String extractAssistantContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    /**
     * 判断回复中是否包含典型工具失败语义。
     */
    private boolean containsToolFailureKeyword(String content) {
        if (content == null || content.isEmpty()) {
            return true;
        }
        return content.contains("工具不可用")
                || content.contains("工具未找到")
                || content.contains("无法调用工具")
                || content.contains("无法查询");
    }

    /**
     * 构建网关 SSE 路径。
     */
    private String buildSseEndpoint(String contextPath, String gatewayId) {
        String safeContext = ensureLeadingSlash(trimTrailingSlash(contextPath));
        String safeGateway = ensureLeadingSlash(trimTrailingSlash(gatewayId));
        return safeContext + safeGateway + "/mcp/sse";
    }

    /**
     * 去掉末尾斜杠，避免 URL 拼接产生双斜杠。
     */
    private String trimTrailingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 确保字符串以斜杠开头。
     */
    private String ensureLeadingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }

    /**
     * 读取字符串配置：优先系统属性，其次环境变量，最后默认值。
     */
    private String getConfig(String propertyKey, String envKey, String defaultValue) {
        String property = System.getProperty(propertyKey);
        if (property != null && !property.isEmpty()) {
            return property;
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return defaultValue;
    }

    /**
     * 读取整型配置，解析失败时回退默认值。
     */
    private Integer getIntConfig(String propertyKey, String envKey, Integer defaultValue) {
        String value = getConfig(propertyKey, envKey, null);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            log.warn("超时配置解析失败，key: {}, value: {}，使用默认值: {}", propertyKey, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 读取 Long 配置，解析失败时回退默认值。
     */
    private Long getLongConfig(String propertyKey, String envKey, Long defaultValue) {
        String value = getConfig(propertyKey, envKey, null);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            log.warn("Long 配置解析失败，key: {}, value: {}，使用默认值: {}", propertyKey, value, defaultValue);
            return defaultValue;
        }
    }

}
