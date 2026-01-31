package com.xbk.knowledge.infrastructure.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.modelcontextprotocol.client.McpSyncClient;

/**
 * 动态 MCP 工具回调提供者
 * 根据运行时注册的 MCP Server 动态提供工具列表
 *
 * 职责：基础设施适配，用于提供可热更新的 ToolCallbackProvider
 * @author xiexu
 */
@Slf4j
@Component
@Primary
public class DynamicMcpToolCallbackProvider implements ToolCallbackProvider {

    private final AtomicReference<List<McpSyncClient>> clients = new AtomicReference<>(Collections.emptyList());
    private volatile ToolCallback[] cachedCallbacks;

    /**
     * 更新 MCP 客户端列表
     *
     * @param mcpClients MCP 客户端列表
     */
    public void updateClients(List<McpSyncClient> mcpClients) {
        List<McpSyncClient> safeClients = mcpClients == null ? Collections.emptyList() : mcpClients;
        clients.set(safeClients);
        cachedCallbacks = null;
        int size = safeClients.size();
        log.info("MCP 工具回调更新完成，当前客户端数量: {}", size);
    }

    /**
     * 返回可用工具回调
     *
     * @return 工具回调列表
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        ToolCallback[] cached = cachedCallbacks;
        if (cached != null) {
            return cached;
        }
        List<McpSyncClient> snapshot = clients.get();
        if (snapshot == null || snapshot.isEmpty()) {
            cachedCallbacks = new ToolCallback[0];
            return cachedCallbacks;
        }
        List<ToolCallback> callbacks = SyncMcpToolCallbackProvider.syncToolCallbacks(snapshot);
        ToolCallback[] result = callbacks.toArray(new ToolCallback[0]);
        cachedCallbacks = result;
        return result;
    }
}
