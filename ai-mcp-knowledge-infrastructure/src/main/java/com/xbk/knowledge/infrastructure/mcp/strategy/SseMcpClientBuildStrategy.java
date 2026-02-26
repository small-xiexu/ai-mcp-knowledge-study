package com.xbk.knowledge.infrastructure.mcp.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;

/**
 * SSE MCP 客户端构建策略
 *
 * 职责：根据 SSE 配置构建 MCP 同步客户端
 *
 * @author sxie
 */
@Component
public class SseMcpClientBuildStrategy extends AbstractMcpClientBuildStrategy {

    /**
     * 创建 SSE MCP 客户端构建策略。
     *
     * @param objectMapper JSON 序列化器。
     */
    public SseMcpClientBuildStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 获取策略类型。
     *
     * @return SSE 策略类型。
     */
    @Override
    public McpClientBuildStrategyType getType() {
        return McpClientBuildStrategyType.SSE;
    }

    /**
     * 根据 SSE 配置构建同步客户端。
     *
     * @param config MCP 服务配置。
     * @return MCP 同步客户端。
     */
    @Override
    public McpSyncClient build(McpServerConfig config) {
        // 获取远程服务地址
        String endpoint = config.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("SSE 模式必须配置 endpoint");
        }
        // 创建 SSE 模式传输
        HttpClientSseClientTransport.Builder builder = HttpClientSseClientTransport.builder(endpoint);
        // 获取 SSE 端点
        String sseEndpoint = config.getSseEndpoint();
        if (StringUtils.hasText(sseEndpoint)) {
            builder.sseEndpoint(sseEndpoint);
        }
        // 解析字符串映射 JSON
        Map<String, String> headers = parseStringMap(config.getHeaders());
        if (!headers.isEmpty()) {
            builder.customizeRequest(requestBuilder -> applyHeaders(requestBuilder, headers));
        }
        // 获取连接超时时间
        int connectTimeoutMs = getConnectTimeoutMs(config.getConnectTimeoutMs());
        builder.connectTimeout(Duration.ofMillis(connectTimeoutMs));
        builder.jsonMapper(getMcpJsonMapper());
        HttpClientSseClientTransport transport = builder.build();
        return buildSyncClient(transport, config);
    }
}
