package com.xbk.knowledge.infrastructure.mcp.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.types.json.JsonMapUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MCP 客户端构建策略抽象基类
 *
 * 职责：提供策略实现共享的解析、超时与客户端构建能力
 *
 * @author sxie
 */
@Slf4j
public abstract class AbstractMcpClientBuildStrategy implements McpClientBuildStrategy {

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * MCP 协议 JSON 映射器。
     */
    private final McpJsonMapper mcpJsonMapper;

    /**
     * 默认连接超时时间（毫秒）。
     */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10000;

    /**
     * 默认请求超时时间（毫秒）。
     */
    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 30000;

    /**
     * 默认初始化超时时间（毫秒）。
     */
    private static final int DEFAULT_INIT_TIMEOUT_MS = 60000;

    /**
     * 创建 MCP 客户端构建策略抽象基类。
     *
     * @param objectMapper JSON 序列化器。
     */
    protected AbstractMcpClientBuildStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.mcpJsonMapper = new JacksonMcpJsonMapper(objectMapper);
    }

    /**
     * 获取 MCP 协议 JSON 映射器。
     *
     * @return MCP 协议 JSON 映射器。
     */
    protected McpJsonMapper getMcpJsonMapper() {
        return mcpJsonMapper;
    }

    /**
     * 获取连接超时时间。
     *
     * @param connectTimeoutMs 配置连接超时时间。
     * @return 有效连接超时时间（毫秒）。
     */
    protected int getConnectTimeoutMs(Integer connectTimeoutMs) {
        return getTimeout(connectTimeoutMs, DEFAULT_CONNECT_TIMEOUT_MS);
    }

    /**
     * 构建同步客户端并统一设置请求与初始化超时。
     *
     * @param transport MCP 客户端传输层。
     * @param config    MCP 服务配置。
     * @return MCP 同步客户端。
     */
    protected McpSyncClient buildSyncClient(McpClientTransport transport, McpServerConfig config) {
        int requestTimeoutMs = getTimeout(config.getRequestTimeoutMs(), DEFAULT_REQUEST_TIMEOUT_MS);
        int initTimeoutMs = getTimeout(config.getInitTimeoutMs(), DEFAULT_INIT_TIMEOUT_MS);
        return McpClient
                .sync(transport)
                .requestTimeout(Duration.ofMillis(requestTimeoutMs))
                .initializationTimeout(Duration.ofMillis(initTimeoutMs))
                .build();
    }

    /**
     * 应用自定义请求头。
     *
     * @param requestBuilder HTTP 请求构建器。
     * @param headers        请求头映射。
     */
    protected void applyHeaders(HttpRequest.Builder requestBuilder, Map<String, String> headers) {
        if (requestBuilder == null || headers == null || headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!StringUtils.hasText(key) || value == null) {
                continue;
            }
            requestBuilder.header(key, value);
        }
    }

    /**
     * 解析字符串数组 JSON。
     *
     * @param json JSON 文本。
     * @return 字符串列表。
     */
    protected List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("解析 MCP args 失败，json: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析字符串映射 JSON。
     *
     * @param json JSON 文本。
     * @return 字符串映射。
     */
    protected Map<String, String> parseStringMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return JsonMapUtils.readStringMap(objectMapper, json);
        } catch (Exception e) {
            log.warn("解析 MCP map 配置失败，json: {}", json, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取有效超时时间。
     *
     * @param value        配置值。
     * @param defaultValue 默认值。
     * @return 超时时间（毫秒）。
     */
    private int getTimeout(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }
}
