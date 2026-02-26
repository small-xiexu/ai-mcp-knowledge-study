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
 * <p>
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
        /**
         * 入口地址支持两种形式：
         * 1、仅主机: http://host:port
         * 2、含路径: http://host:port/custom/mcp?x=1
         */
        String endpoint = config.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("HTTP 模式必须配置 endpoint");
        }

        // SDK builder 需要 baseUri 与 endpointPath 分开配置，这里先准备默认值。
        String baseUri = endpoint;
        String endpointPath = null;
        try {
            // 尝试把完整 URL 拆成“主机部分 + 路径部分”，兼容网关自定义路径。
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

                // 路径和查询参数作为 MCP endpoint，避免丢失路由信息。
                endpointPath = uri.getRawPath();
                if (StringUtils.hasText(uri.getRawQuery())) {
                    endpointPath = endpointPath + "?" + uri.getRawQuery();
                }
            }
        } catch (Exception e) {
            // URL 非标准时不阻断流程，继续用原始 endpoint 让下游自行处理。
            log.warn("HTTP endpoint 解析失败，回退为原始值: {}", endpoint, e);
        }

        // 先用主机信息创建 transport builder，再按需覆盖 endpoint 路径。
        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(baseUri);
        if (StringUtils.hasText(endpointPath)) {
            builder.endpoint(endpointPath);
        }

        // 自定义 header 用于鉴权或租户透传（如 Authorization / X-Tenant-Id）。
        Map<String, String> headers = parseStringMap(config.getHeaders());
        if (!headers.isEmpty()) {
            builder.customizeRequest(requestBuilder -> applyHeaders(requestBuilder, headers));
        }

        // 连接超时只影响建连阶段；请求与初始化超时在 buildSyncClient 里统一设置。
        int connectTimeoutMs = getConnectTimeoutMs(config.getConnectTimeoutMs());
        builder.connectTimeout(Duration.ofMillis(connectTimeoutMs));
        builder.jsonMapper(getMcpJsonMapper());

        // 构建 transport 后交给基类统一封装同步客户端。
        HttpClientStreamableHttpTransport transport = builder.build();
        return buildSyncClient(transport, config);
    }
}
