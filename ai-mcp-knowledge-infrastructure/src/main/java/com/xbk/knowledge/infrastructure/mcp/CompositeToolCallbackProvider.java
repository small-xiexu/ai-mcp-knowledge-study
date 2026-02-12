package com.xbk.knowledge.infrastructure.mcp;

import com.xbk.knowledge.infrastructure.gateway.GatewayToolCallbackProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 组合工具回调提供者
 * 合并 Dynamic MCP 工具与 Gateway HTTP 工具
 *
 * @author xiexu
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class CompositeToolCallbackProvider implements ToolCallbackProvider {

    private final DynamicMcpToolCallbackProvider dynamicProvider;
    private final GatewayToolCallbackProvider gatewayProvider;

    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> merged = new ArrayList<>();
        Set<String> names = new HashSet<>();

        appendCallbacks(merged, names, dynamicProvider == null ? null : dynamicProvider.getToolCallbacks());
        appendCallbacks(merged, names, gatewayProvider == null ? null : gatewayProvider.getToolCallbacks());

        return merged.toArray(new ToolCallback[0]);
    }

    private void appendCallbacks(List<ToolCallback> merged,
                                 Set<String> names,
                                 ToolCallback[] callbacks) {
        if (callbacks == null || callbacks.length == 0) {
            return;
        }
        for (ToolCallback callback : callbacks) {
            if (callback == null || callback.getToolDefinition() == null) {
                continue;
            }
            String toolName = callback.getToolDefinition().name();
            if (toolName == null || toolName.isBlank()) {
                continue;
            }
            if (!names.add(toolName)) {
                log.warn("发现重名工具，已忽略后续定义: {}", toolName);
                continue;
            }
            merged.add(callback);
        }
    }
}
