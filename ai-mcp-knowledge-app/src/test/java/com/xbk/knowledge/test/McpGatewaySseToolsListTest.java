package com.xbk.knowledge.test;

import com.xbk.knowledge.Application;
import com.xbk.knowledge.application.service.runtime.McpServerRuntimeService;
import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.types.enums.McpServerType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MCP Gateway SSE + tools/list 集成测试
 * 目标：验证 MCP 客户端可以通过 SSE 建立会话并拉取工具清单
 *
 * 说明：
 * 本测试不依赖任何大模型，仅使用 MCP 客户端完成连接与工具清单拉取。
 *
 * 前置条件：
 * 1. ai-mcp-gateway-study 服务已启动（默认 http://localhost:8091）
 * 2. gatewayId 对应的工具已配置并可用（默认 gateway_001）
 *
 * 可通过系统属性或环境变量覆盖默认值：
 * - baseUrl: mcp.gateway.base-url / MCP_GATEWAY_BASE_URL
 * - contextPath: mcp.gateway.context-path / MCP_GATEWAY_CONTEXT_PATH
 * - gatewayId: mcp.gateway.gateway-id / MCP_GATEWAY_ID
 * - sseEndpoint: mcp.gateway.sse-endpoint / MCP_GATEWAY_SSE_ENDPOINT
 * - toolName: mcp.gateway.tool-name / MCP_GATEWAY_TOOL_NAME
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

    private final McpServerRuntimeService runtimeService;
    private final ToolCallbackProvider toolCallbackProvider;

    @Autowired
    public McpGatewaySseToolsListTest(McpServerRuntimeService runtimeService,
                                      ToolCallbackProvider toolCallbackProvider) {
        this.runtimeService = runtimeService;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @Test
    public void test_sse_tools_list_from_gateway() {
        String baseUrl = getConfig("mcp.gateway.base-url", "MCP_GATEWAY_BASE_URL", "http://localhost:8091");
        String contextPath = getConfig("mcp.gateway.context-path", "MCP_GATEWAY_CONTEXT_PATH", "/api-gateway");
        String gatewayId = getConfig("mcp.gateway.gateway-id", "MCP_GATEWAY_ID", "gateway_001");
        String defaultSseEndpoint = buildSseEndpoint(contextPath, gatewayId);
        String sseEndpoint = getConfig("mcp.gateway.sse-endpoint", "MCP_GATEWAY_SSE_ENDPOINT", defaultSseEndpoint);
        String expectedToolName = getConfig("mcp.gateway.tool-name", "MCP_GATEWAY_TOOL_NAME", "JavaSDKMCPClient_getCompanyEmployee");
        Integer connectTimeoutMs = getIntConfig("mcp.gateway.connect-timeout-ms", "MCP_GATEWAY_CONNECT_TIMEOUT_MS", 10000);
        Integer requestTimeoutMs = getIntConfig("mcp.gateway.request-timeout-ms", "MCP_GATEWAY_REQUEST_TIMEOUT_MS", 120000);
        Integer initTimeoutMs = getIntConfig("mcp.gateway.init-timeout-ms", "MCP_GATEWAY_INIT_TIMEOUT_MS", 300000);

        String endpoint = trimTrailingSlash(baseUrl);
        Long configId = System.currentTimeMillis();

        log.info("MCP SSE 测试配置 baseUrl: {}, sseEndpoint: {}, gatewayId: {}", endpoint, sseEndpoint, gatewayId);
        log.info("MCP SSE 超时配置 connect: {}ms, request: {}ms, init: {}ms",
                connectTimeoutMs, requestTimeoutMs, initTimeoutMs);

        // 构建 MCP Server 配置（SSE）
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
            // 运行时注册 MCP 连接（触发 initialize + tools/list）
            runtimeService.registerOrUpdate(config);

            // 通过 ToolCallbackProvider 获取工具清单（对应 tools/list 响应）
            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            assertNotNull(callbacks, "❌ ToolCallbackProvider 未返回工具");
            assertTrue(callbacks.length > 0, "❌ tools/list 未返回任何工具");

            // 校验目标工具是否存在
            boolean matched = hasTool(callbacks, expectedToolName);
            assertTrue(matched, "❌ 未找到期望工具: " + expectedToolName);

            log.info("✅ tools/list 校验成功，工具数量: {}", callbacks.length);
        } finally {
            // 释放连接，避免资源泄露
            runtimeService.unregister(configId);
        }
    }

    private boolean hasTool(ToolCallback[] callbacks, String expectedName) {
        if (callbacks == null || callbacks.length == 0 || expectedName == null) {
            return false;
        }
        for (ToolCallback callback : callbacks) {
            if (callback == null) {
                continue;
            } else {
                callback.getToolDefinition();
            }
            String name = callback.getToolDefinition().name();
            if (expectedName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String buildSseEndpoint(String contextPath, String gatewayId) {
        String safeContext = ensureLeadingSlash(trimTrailingSlash(contextPath));
        String safeGateway = ensureLeadingSlash(trimTrailingSlash(gatewayId));
        return safeContext + safeGateway + "/mcp/sse";
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String ensureLeadingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }

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
}
