package com.xbk.knowledge.infrastructure.mcp;

import com.xbk.knowledge.application.service.runtime.McpServerRuntimeService;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.infrastructure.mcp.strategy.McpClientBuildStrategy;
import com.xbk.knowledge.infrastructure.mcp.strategy.McpClientBuildStrategyFactory;
import com.xbk.knowledge.infrastructure.mcp.strategy.McpClientBuildStrategyType;
import com.xbk.knowledge.types.enums.McpServerType;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server 运行时管理实现
 * 负责按配置动态创建与管理 MCP 客户端连接
 * <p>
 * 职责：基础设施实现，用于连接 MCP Server 并维护运行状态
 *
 * @author sxie
 */
@Slf4j
@Service
public class McpServerRuntimeServiceImpl implements McpServerRuntimeService {

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
     * 运行时配置快照注册表。
     */
    private final Map<Long, RuntimeConfigSnapshot> configSnapshotRegistry = new ConcurrentHashMap<>();

    /**
     * MCP 客户端构建策略工厂。
     */
    private final McpClientBuildStrategyFactory clientBuildStrategyFactory;

    /**
     * 创建 MCP 服务运行时并注入依赖组件。
     *
     * @param toolCallbackProvider 工具回调提供器。
     * @param clientBuildStrategyFactory MCP 客户端构建策略工厂。
     */
    public McpServerRuntimeServiceImpl(DynamicMcpToolCallbackProvider toolCallbackProvider,
                                       McpClientBuildStrategyFactory clientBuildStrategyFactory) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.clientBuildStrategyFactory = clientBuildStrategyFactory;
    }

    /**
     * 注册或更新 MCP Server 连接
     * 根据配置类型创建客户端并完成初始化
     * <p>
     * 配置变更后需要重建运行时连接
     *
     * @param config 配置信息。
     */
    @Override
    public void registerOrUpdate(McpServerConfig config) {
        registerOrUpdateInternal(config, true);
    }

    /**
     * 取消注册 MCP Server 连接
     * 释放客户端资源并刷新工具回调
     * <p>
     * 禁用或删除配置时需要释放连接
     *
     * @param id 主键 ID。
     */
    @Override
    public void unregister(Long id) {
        unregisterInternal(id, true);
    }

    /**
     * 刷新所有启用 MCP Server 连接
     * <p>
     * 大白话：
     * 1、先把入参收敛为安全列表，并按 configId 去重（同 ID 以后出现的为准）；
     * 2、逐条执行注册/更新（内部按快照判定是否真的重连）；
     * 3、把运行中但本次配置里不存在的连接做差集下线；
     * 4、批处理结束后仅在发生变更时刷新一次工具回调。
     *
     * @param configs 启用状态的 MCP Server 配置列表。
     */
    @Override
    public void refresh(List<McpServerConfig> configs) {
        // 兜底空入参，避免后续遍历出现空指针。
        List<McpServerConfig> safeConfigs = Optional.ofNullable(configs).orElse(Collections.emptyList());

        // 按配置 ID 去重；同一 ID 多次出现时以后者覆盖前者。
        Map<Long, McpServerConfig> deduplicatedConfigs = new LinkedHashMap<>();
        for (McpServerConfig config : safeConfigs) {
            // 无效配置（空对象或缺少主键）直接跳过。
            if (config == null || config.getId() == null) {
                continue;
            }
            deduplicatedConfigs.put(config.getId(), config);
        }

        // 记录本次刷新是否发生运行态变更，用于控制是否触发回调刷新。
        boolean changed = false;
        // 对当前启用配置逐条做注册/更新，内部会按快照判定是否真的重连。
        for (McpServerConfig config : deduplicatedConfigs.values()) {
            changed = registerOrUpdateInternal(config, false) || changed;
        }

        // 计算差集：运行中存在但本次配置不存在的连接，需要下线回收。
        Set<Long> staleIds = new HashSet<>(clientRegistry.keySet());
        staleIds.removeAll(deduplicatedConfigs.keySet());
        for (Long id : staleIds) {
            changed = unregisterInternal(id, false) || changed;
        }

        // 仅在有变更时刷新一次工具回调，避免无效刷新。
        if (changed) {
            refreshToolCallbacks();
        }
    }

    /**
     * 判断 MCP Server 是否处于运行状态
     * <p>
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
     * <p>
     * 应用关闭时释放外部连接
     */
    @PreDestroy
    public void shutdown() {
        for (McpSyncClient client : clientRegistry.values()) {
            closeQuietly(client);
        }
        clientRegistry.clear();
        metaRegistry.clear();
        configSnapshotRegistry.clear();
        refreshToolCallbacks();
    }

    /**
     * 注册或更新 MCP Server 连接。
     * <p>
     * 大白话：
     * 1、配置为空或无主键直接忽略；
     * 2、只有 enabled=true 才保留运行态，否则走注销；
     * 3、对比运行时快照，未变化时跳过重连；
     * 4、需要重连时先回收旧连接，再按最新配置建连并 initialize；
     * 5、建连成功后写回三本注册表，并按需刷新工具回调。
     *
     * @param config           配置信息。
     * @param refreshCallbacks 是否立即刷新工具回调。
     * @return `true` 表示运行时状态发生变化，`false` 表示无变化。
     */
    private boolean registerOrUpdateInternal(McpServerConfig config, boolean refreshCallbacks) {
        // 空配置直接忽略，不触发任何运行态变更。
        if (config == null) {
            return false;
        }
        // 主键为空无法建立运行态索引，直接返回无变化。
        Long configId = config.getId();
        if (configId == null) {
            return false;
        }
        // 非启用配置统一按下线路径处理，保持启停语义一致。
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return unregisterInternal(configId, refreshCallbacks);
        }

        // 构建最新快照，与当前运行态快照比较，判定是否需要重建连接。
        RuntimeConfigSnapshot newSnapshot = buildRuntimeConfigSnapshot(config);
        RuntimeConfigSnapshot currentSnapshot = configSnapshotRegistry.get(configId);
        // 快照未变化且三本注册表完整时，跳过重连。
        if (newSnapshot.equals(currentSnapshot)
                && clientRegistry.containsKey(configId)
                && metaRegistry.containsKey(configId)) {
            return false;
        }

        // 先回收旧连接和旧元数据，避免重复连接与脏状态残留。
        McpSyncClient existing = clientRegistry.remove(configId);
        closeQuietly(existing);
        metaRegistry.remove(configId);

        // 按最新配置重建客户端
        McpSyncClient client = buildClient(config);
        // 执行 MCP initialize 握手
        client.initialize();
        // 建连成功后写回运行态注册表与快照表。
        clientRegistry.put(configId, client);
        metaRegistry.put(configId, new McpServerMeta(config.getServerName()));
        configSnapshotRegistry.put(configId, newSnapshot);
        // 按需触发工具回调刷新，保证工具可见性与运行态一致。
        if (refreshCallbacks) {
            refreshToolCallbacks();
        }
        log.info("MCP Server 已注册，id: {}, name: {}", configId, config.getServerName());
        return true;
    }

    /**
     * 取消注册 MCP Server 连接。
     * <p>
     * 大白话：
     * 1、主键为空直接返回无变化；
     * 2、从 client/meta/snapshot 三本运行态注册表同时移除；
     * 3、若原本就不存在则不触发后续动作；
     * 4、若存在连接则优雅关闭，并按需刷新工具回调。
     *
     * @param id               主键 ID。
     * @param refreshCallbacks 是否立即刷新工具回调。
     * @return `true` 表示运行时状态发生变化，`false` 表示无变化。
     */
    private boolean unregisterInternal(Long id, boolean refreshCallbacks) {
        if (id == null) {
            return false;
        }
        McpSyncClient client = clientRegistry.remove(id);
        McpServerMeta removedMeta = metaRegistry.remove(id);
        RuntimeConfigSnapshot removedSnapshot = configSnapshotRegistry.remove(id);
        if (client == null && removedMeta == null && removedSnapshot == null) {
            return false;
        }
        closeQuietly(client);
        if (refreshCallbacks) {
            refreshToolCallbacks();
        }
        log.info("MCP Server 已注销，id: {}", id);
        return true;
    }

    /**
     * 构建 MCP 客户端
     * <p>
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
        McpClientBuildStrategyType strategyType = McpClientBuildStrategyType.from(serverType);
        McpClientBuildStrategy strategy = clientBuildStrategyFactory.getStrategy(strategyType);
        return strategy.build(config);
    }

    /**
     * 安静关闭客户端
     * <p>
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
     * <p>
     * 大白话：
     * 1、把运行中的 clientRegistry 转成 provider 识别的 descriptor 列表；
     * 2、仅保留 client 和 meta 都完整的运行态记录；
     * 3、一次性替换 provider 快照，让工具可见性与运行态收敛一致。
     */
    private void refreshToolCallbacks() {
        // 重新收集当前运行中的客户端描述符，构建 provider 可识别的快照。
        ArrayList<DynamicMcpToolCallbackProvider.McpClientDescriptor> descriptors = new ArrayList<>();
        for (Map.Entry<Long, McpSyncClient> entry : clientRegistry.entrySet()) {
            // 通过 configId 对齐客户端实例与元信息（serverName）
            Long configId = entry.getKey();
            // client 来自 clientRegistry 的 value，表示当前已建连的 MCP 客户端实例
            McpSyncClient client = entry.getValue();
            // meta 从 metaRegistry 按 configId 取回，提供服务名等展示与追踪信息
            McpServerMeta meta = metaRegistry.get(configId);
            // 客户端或元信息缺失时跳过，避免生成不完整描述符
            if (meta == null || client == null) {
                continue;
            }
            // 封装为 DynamicMcpToolCallbackProvider 使用的描述符对象
            DynamicMcpToolCallbackProvider.McpClientDescriptor mcpClientDescriptor = new DynamicMcpToolCallbackProvider.McpClientDescriptor(meta.serverName, client);
            descriptors.add(mcpClientDescriptor);
        }
        // 原子替换 provider 客户端快照，触发工具回调缓存重建
        toolCallbackProvider.updateClients(descriptors);
    }

    /**
     * 构建运行时配置快照。
     * <p>
     * 仅包含会影响连接与工具暴露的字段
     *
     * @param config 配置信息。
     * @return 运行时配置快照。
     */
    private RuntimeConfigSnapshot buildRuntimeConfigSnapshot(McpServerConfig config) {
        return new RuntimeConfigSnapshot(
                config.getServerName(),
                config.getServerType(),
                config.getEnabled(),
                config.getCommand(),
                config.getArgs(),
                config.getEnv(),
                config.getEndpoint(),
                config.getSseEndpoint(),
                config.getHeaders(),
                config.getConnectTimeoutMs(),
                config.getRequestTimeoutMs(),
                config.getInitTimeoutMs()
        );
    }

    /**
     * 运行时配置快照。
     * <p>
     * 用于判定配置是否变化，避免无效重连
     */
    private static final class RuntimeConfigSnapshot {
        /**
         * MCP 服务名称。
         */
        private final String serverName;

        /**
         * MCP 服务类型。
         */
        private final McpServerType serverType;

        /**
         * 是否启用。
         */
        private final Boolean enabled;

        /**
         * STDIO 命令。
         */
        private final String command;

        /**
         * STDIO 参数。
         */
        private final String args;

        /**
         * STDIO 环境变量。
         */
        private final String env;

        /**
         * 服务端点。
         */
        private final String endpoint;

        /**
         * SSE 端点。
         */
        private final String sseEndpoint;

        /**
         * HTTP 请求头。
         */
        private final String headers;

        /**
         * 连接超时时间。
         */
        private final Integer connectTimeoutMs;

        /**
         * 请求超时时间。
         */
        private final Integer requestTimeoutMs;

        /**
         * 初始化超时时间。
         */
        private final Integer initTimeoutMs;

        /**
         * 创建运行时配置快照对象。
         *
         * @param serverName       MCP 服务名称。
         * @param serverType       MCP 服务类型。
         * @param enabled          是否启用。
         * @param command          STDIO 命令。
         * @param args             STDIO 参数。
         * @param env              STDIO 环境变量。
         * @param endpoint         服务端点。
         * @param sseEndpoint      SSE 端点。
         * @param headers          HTTP 请求头。
         * @param connectTimeoutMs 连接超时时间。
         * @param requestTimeoutMs 请求超时时间。
         * @param initTimeoutMs    初始化超时时间。
         */
        private RuntimeConfigSnapshot(String serverName,
                                      McpServerType serverType,
                                      Boolean enabled,
                                      String command,
                                      String args,
                                      String env,
                                      String endpoint,
                                      String sseEndpoint,
                                      String headers,
                                      Integer connectTimeoutMs,
                                      Integer requestTimeoutMs,
                                      Integer initTimeoutMs) {
            this.serverName = serverName;
            this.serverType = serverType;
            this.enabled = enabled;
            this.command = command;
            this.args = args;
            this.env = env;
            this.endpoint = endpoint;
            this.sseEndpoint = sseEndpoint;
            this.headers = headers;
            this.connectTimeoutMs = connectTimeoutMs;
            this.requestTimeoutMs = requestTimeoutMs;
            this.initTimeoutMs = initTimeoutMs;
        }

        /**
         * 判断快照是否等价。
         *
         * @param obj 比较对象。
         * @return `true` 表示等价，`false` 表示不等价。
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RuntimeConfigSnapshot)) {
                return false;
            }
            RuntimeConfigSnapshot that = (RuntimeConfigSnapshot) obj;
            return Objects.equals(serverName, that.serverName)
                    && serverType == that.serverType
                    && Objects.equals(enabled, that.enabled)
                    && Objects.equals(command, that.command)
                    && Objects.equals(args, that.args)
                    && Objects.equals(env, that.env)
                    && Objects.equals(endpoint, that.endpoint)
                    && Objects.equals(sseEndpoint, that.sseEndpoint)
                    && Objects.equals(headers, that.headers)
                    && Objects.equals(connectTimeoutMs, that.connectTimeoutMs)
                    && Objects.equals(requestTimeoutMs, that.requestTimeoutMs)
                    && Objects.equals(initTimeoutMs, that.initTimeoutMs);
        }

        /**
         * 计算快照哈希值。
         *
         * @return 哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(serverName, serverType, enabled, command, args, env,
                    endpoint, sseEndpoint, headers, connectTimeoutMs, requestTimeoutMs, initTimeoutMs);
        }
    }

    /**
     * MCP 服务元数据。
     */
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
