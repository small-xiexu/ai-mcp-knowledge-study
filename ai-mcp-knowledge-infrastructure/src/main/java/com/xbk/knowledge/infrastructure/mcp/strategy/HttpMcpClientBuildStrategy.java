package com.xbk.knowledge.infrastructure.mcp.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP MCP 客户端构建策略
 *
 * 职责：根据 HTTP 配置构建 MCP 同步客户端
 *
 * @author sxie
 */
@Slf4j
@Component
public class HttpMcpClientBuildStrategy extends AbstractMcpClientBuildStrategy {

    /**
     * 创建 HTTP MCP 客户端构建策略。
     *
     * @param objectMapper JSON 序列化器。
     */
    public HttpMcpClientBuildStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 获取策略类型。
     *
     * @return HTTP 策略类型。
     */
    @Override
    public McpClientBuildStrategyType getType() {
        return McpClientBuildStrategyType.HTTP;
    }

    /**
     * 根据 HTTP 配置构建同步客户端。
     *
     * @param config MCP 服务配置。
     * @return MCP 同步客户端。
     */
    @Override
    public McpSyncClient build(McpServerConfig config) {
        String endpoint = config.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("HTTP 模式必须配置 endpoint");
        }

        String baseUri = endpoint;
        String endpointPath = null;
        try {
            URI uri = URI.create(endpoint);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            if (StringUtils.hasText(scheme) && StringUtils.hasText(host)) {
                StringBuilder baseBuilder = new StringBuilder();
                baseBuilder.append(scheme).append("://").append(host);
                if (port > 0) {
                    baseBuilder.append(":").append(port);
                }
                baseUri = baseBuilder.toString();
                endpointPath = uri.getRawPath();
                if (StringUtils.hasText(uri.getRawQuery())) {
                    endpointPath = endpointPath + "?" + uri.getRawQuery();
                }
            }
        } catch (Exception e) {
            log.warn("HTTP endpoint 解析失败，回退为原始值: {}", endpoint, e);
        }

        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(baseUri);
        if (StringUtils.hasText(endpointPath)) {
            builder.endpoint(endpointPath);
        }

        Map<String, String> headers = parseStringMap(config.getHeaders());
        if (!headers.isEmpty()) {
            builder.customizeRequest(requestBuilder -> applyHeaders(requestBuilder, headers));
        }

        int connectTimeoutMs = getConnectTimeoutMs(config.getConnectTimeoutMs());
        builder.connectTimeout(Duration.ofMillis(connectTimeoutMs));
        builder.jsonMapper(getMcpJsonMapper());
        HttpClientStreamableHttpTransport transport = builder.build();
        return buildSyncClient(transport, config);
    }
}
