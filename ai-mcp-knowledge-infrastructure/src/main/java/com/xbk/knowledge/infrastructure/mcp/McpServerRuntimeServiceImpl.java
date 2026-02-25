package com.xbk.knowledge.infrastructure.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.runtime.McpServerRuntimeService;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.types.enums.McpServerType;
import com.xbk.knowledge.types.json.JsonMapUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server 运行时管理实现
 * 负责按配置动态创建与管理 MCP 客户端连接
 *
 * 职责：基础设施实现，用于连接 MCP Server 并维护运行状态
 * @author sxie
 */
@Slf4j
@Service
public class McpServerRuntimeServiceImpl implements McpServerRuntimeService {

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
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 动态工具回调提供器。
     */
    private final DynamicMcpToolCallbackProvider toolCallbackProvider;

    /**
     * 运行中的 MCP 客户端注册表。
     */
    private final Map<Long, McpSyncClient> clientRegistry = new ConcurrentHashMap<>();

    /**
     * MCP 服务元数据注册表。
     */
    private final Map<Long, McpServerMeta> metaRegistry = new ConcurrentHashMap<>();

    /**
     * MCP 协议 JSON 映射器。
     */
    private final McpJsonMapper mcpJsonMapper;

    /**
     * 创建 MCP 服务运行时并注入依赖组件。
     * 
     * @param objectMapper JSON序列化器。
     * @param toolCallbackProvider 工具回调提供器。
     */
    public McpServerRuntimeServiceImpl(ObjectMapper objectMapper,
                                       DynamicMcpToolCallbackProvider toolCallbackProvider) {
        this.objectMapper = objectMapper;
        this.toolCallbackProvider = toolCallbackProvider;
        this.mcpJsonMapper = new JacksonMcpJsonMapper(objectMapper);
    }

    /**
     * 注册或更新 MCP Server 连接
     * 根据配置类型创建客户端并完成初始化
     *
     * 配置变更后需要重建运行时连接
     * 
     * @param config 配置信息。
     */
    @Override
    public void registerOrUpdate(McpServerConfig config) {
        if (config == null) {
            return;
        }
        Long configId = config.getId();
        if (configId == null) {
            return;
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            unregister(configId);
            return;
        }

        // 先关闭旧连接，避免资源泄露
        McpSyncClient existing = clientRegistry.remove(configId);
        closeQuietly(existing);
        metaRegistry.remove(configId);

        // 按配置创建客户端并完成初始化
        McpSyncClient client = buildClient(config);
        client.initialize();
        clientRegistry.put(configId, client);
        metaRegistry.put(configId, new McpServerMeta(config.getServerName()));
        refreshToolCallbacks();
        log.info("MCP Server 已注册，id: {}, name: {}", configId, config.getServerName());
    }

    /**
     * 取消注册 MCP Server 连接
     * 释放客户端资源并刷新工具回调
     *
     * 禁用或删除配置时需要释放连接
     * 
     * @param id 主键 ID。
     */
    @Override
    public void unregister(Long id) {
        if (id == null) {
            return;
        }
        McpSyncClient client = clientRegistry.remove(id);
        metaRegistry.remove(id);
        closeQuietly(client);
        refreshToolCallbacks();
        log.info("MCP Server 已注销，id: {}", id);
    }

    /**
     * 刷新所有启用 MCP Server 连接
     * 以配置列表为准重建运行时连接
     *
     * 批量配置变更时统一刷新运行时连接
     * 
     * @param configs 启用状态的 MCP Server 配置列表。
     */
    @Override
    public void refresh(List<McpServerConfig> configs) {
        List<McpServerConfig> safeConfigs = configs == null ? Collections.emptyList() : configs;
        Set<Long> activeIds = ConcurrentHashMap.newKeySet();
        for (McpServerConfig config : safeConfigs) {
            if (config == null || config.getId() == null) {
                continue;
            }
            activeIds.add(config.getId());
            registerOrUpdate(config);
        }

        for (Long id : clientRegistry.keySet()) {
            if (!activeIds.contains(id)) {
                unregister(id);
            }
        }
    }

    /**
     * 判断 MCP Server 是否处于运行状态
     *
     * 提供运行状态探测能力
     * 
     * @param id 主键 ID。
     * @return `true` 表示运行中，`false` 表示未运行。
     */
    @Override
    public boolean isRunning(Long id) {
        if (id == null) {
            return false;
        }
        return clientRegistry.containsKey(id);
    }

    /**
     * 关闭所有 MCP 客户端连接
     *
     * 应用关闭时释放外部连接
     */
    @PreDestroy
    public void shutdown() {
        for (McpSyncClient client : clientRegistry.values()) {
            closeQuietly(client);
        }
        clientRegistry.clear();
        metaRegistry.clear();
        refreshToolCallbacks();
    }

    /**
     * 构建 MCP 客户端
     *
     * 根据不同协议创建对应的 Transport
     * 
     * @param config 配置信息。
     * @return MCP 同步客户端。
     */
    private McpSyncClient buildClient(McpServerConfig config) {
        McpServerType serverType = config.getServerType();
        if (serverType == null) {
            throw new IllegalArgumentException("MCP Server 类型不能为空");
        }
        switch (serverType) {
            case STDIO:
                return buildStdioClient(config);
            case SSE:
                return buildSseClient(config);
            case HTTP:
                return buildHttpClient(config);
            case WEBSOCKET:
                throw new IllegalArgumentException("当前版本暂不支持 WEBSOCKET 类型");
            default:
                throw new IllegalArgumentException("不支持的 MCP Server 类型: " + serverType);
        }
    }

    /**
     * 构建 STDIO 客户端
     *
     * STDIO 模式需命令、参数、环境变量
     * 
     * @param config 配置信息。
     * @return STDIO MCP 同步客户端。
     */
    private McpSyncClient buildStdioClient(McpServerConfig config) {
        String command = config.getCommand();
        if (!StringUtils.hasText(command)) {
            throw new IllegalArgumentException("STDIO 模式必须配置 command");
        }
        List<String> args = parseStringList(config.getArgs());
        Map<String, String> env = parseStringMap(config.getEnv());

        ServerParameters parameters = ServerParameters
                .builder(command)
                .args(args)
                .env(env)
                .build();
        StdioClientTransport transport = new StdioClientTransport(parameters, mcpJsonMapper);
        return buildSyncClient(transport, config);
    }

    /**
     * 构建 SSE 客户端
     *
     * SSE 模式需 endpoint 与超时配置
     * 
     * @param config 配置信息。
     * @return SSE MCP 同步客户端。
     */
    private McpSyncClient buildSseClient(McpServerConfig config) {
        String endpoint = config.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("SSE 模式必须配置 endpoint");
        }

        HttpClientSseClientTransport.Builder builder = HttpClientSseClientTransport.builder(endpoint);
        String sseEndpoint = config.getSseEndpoint();
        if (StringUtils.hasText(sseEndpoint)) {
            builder.sseEndpoint(sseEndpoint);
        }

        Map<String, String> headers = parseStringMap(config.getHeaders());
        if (!headers.isEmpty()) {
            builder.customizeRequest(requestBuilder -> applyHeaders(requestBuilder, headers));
        }

        int connectTimeoutMs = getTimeout(config.getConnectTimeoutMs(), DEFAULT_CONNECT_TIMEOUT_MS);
        builder.connectTimeout(Duration.ofMillis(connectTimeoutMs));
        builder.jsonMapper(mcpJsonMapper);

        HttpClientSseClientTransport transport = builder.build();
        return buildSyncClient(transport, config);
    }

    /**
     * 构建 HTTP 客户端
     *
     * HTTP 模式需 endpoint 与超时配置
     * 
     * @param config 配置信息。
     * @return HTTP MCP 同步客户端。
     */
    private McpSyncClient buildHttpClient(McpServerConfig config) {
        String endpoint = config.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("HTTP 模式必须配置 endpoint");
        }
        // 支持配置完整 URL（包含路径），避免 SDK 固定使用 /mcp 造成 404
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

        int connectTimeoutMs = getTimeout(config.getConnectTimeoutMs(), DEFAULT_CONNECT_TIMEOUT_MS);
        builder.connectTimeout(Duration.ofMillis(connectTimeoutMs));
        builder.jsonMapper(mcpJsonMapper);

        HttpClientStreamableHttpTransport transport = builder.build();
        return buildSyncClient(transport, config);
    }

    /**
     * 构建同步客户端
     *
     * 统一设置请求/初始化超时
     * 
     * @param transport MCP 客户端传输层。
     * @param config 配置信息。
     * @return 初始化后的 MCP 同步客户端。
     */
    private McpSyncClient buildSyncClient(McpClientTransport transport,
                                          McpServerConfig config) {
        int requestTimeoutMs = getTimeout(config.getRequestTimeoutMs(), DEFAULT_REQUEST_TIMEOUT_MS);
        int initTimeoutMs = getTimeout(config.getInitTimeoutMs(), DEFAULT_INIT_TIMEOUT_MS);
        return McpClient
                .sync(transport)
                .requestTimeout(Duration.ofMillis(requestTimeoutMs))
                .initializationTimeout(Duration.ofMillis(initTimeoutMs))
                .build();
    }

    /**
     * 应用自定义 Header
     *
     * 支持鉴权等自定义请求头
     * 
     * @param requestBuilder HTTP 请求构建器。
     * @param headers 请求头映射。
     */
    private void applyHeaders(HttpRequest.Builder requestBuilder, Map<String, String> headers) {
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
     * 解析 JSON 数组
     *
     * 配置使用 JSON 存储需要还原
     * 
     * @param json JSON 文本。
     * @return 字符串列表。
     */
    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 MCP args 失败，json: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析 JSON 对象
     *
     * 配置使用 JSON 存储需要还原
     * 
     * @param json JSON 文本。
     */
    private Map<String, String> parseStringMap(String json) {
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
     * 获取超时时间
     *
     * 兜底非法值，避免传入负数
     * 
     * @param value 值。
     * @param defaultValue 默认值。
     * @return 超时时间（毫秒）。
     */
    private int getTimeout(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    /**
     * 安静关闭客户端
     *
     * 避免关闭异常影响主流程
     * 
     * @param client MCP 同步客户端。
     */
    private void closeQuietly(McpSyncClient client) {
        if (client == null) {
            return;
        }
        try {
            client.closeGracefully();
        } catch (Exception e) {
            log.warn("关闭 MCP 客户端失败", e);
        }
    }

    /**
     * 刷新工具回调
     *
     * 客户端变更后需要同步工具列表
     */
    private void refreshToolCallbacks() {
        ArrayList<DynamicMcpToolCallbackProvider.McpClientDescriptor> descriptors = new ArrayList<>();
        for (Map.Entry<Long, McpSyncClient> entry : clientRegistry.entrySet()) {
            Long configId = entry.getKey();
            McpSyncClient client = entry.getValue();
            McpServerMeta meta = metaRegistry.get(configId);
            if (meta == null || client == null) {
                continue;
            }
            descriptors.add(new DynamicMcpToolCallbackProvider.McpClientDescriptor(meta.serverName, client));
        }
        toolCallbackProvider.updateClients(descriptors);
    }

    private static final class McpServerMeta {
        /**
         * MCP 服务名称。
         */
        private final String serverName;

        private McpServerMeta(String serverName) {
            this.serverName = serverName;
        }
    }
}
