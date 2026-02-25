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
 * 合并 Dynamic MCP 工具与 Gateway HTTP 工具，作为 Spring AI 的 Primary ToolCallbackProvider
 * <p>
 * 系统同时存在两种工具来源——MCP Server 动态注册的工具和 Gateway HTTP 配置的工具，
 * 需要统一合并后提供给 ChatClient，同时处理工具名称去重
 *
 * @author sxie
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class CompositeToolCallbackProvider implements ToolCallbackProvider {

    /**
     * 动态 MCP 工具回调提供器。
     */
    private final DynamicMcpToolCallbackProvider dynamicProvider;

    /**
     * 网关工具回调提供器。
     */
    private final GatewayToolCallbackProvider gatewayProvider;

    /**
     * 合并两个来源的工具回调，按工具名称去重（先注册的优先）
     *
     * @return 聚合后的工具回调数组。
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> merged = new ArrayList<>();
        Set<String> names = new HashSet<>();

        ToolCallback[] dynamicCallbacks = null;
        if (dynamicProvider != null) {
            dynamicCallbacks = dynamicProvider.getToolCallbacks();
        }
        appendCallbacks(merged, names, dynamicCallbacks);

        ToolCallback[] gatewayCallbacks = null;
        if (gatewayProvider != null) {
            gatewayCallbacks = gatewayProvider.getToolCallbacks();
        }
        appendCallbacks(merged, names, gatewayCallbacks);

        return merged.toArray(new ToolCallback[0]);
    }

    /**
     * 将回调数组追加到合并列表，跳过空值和重名工具
     *
     * @param merged    合并后的回调列表。
     * @param names     已收集的工具名称集合。
     * @param callbacks 待追加的工具回调数组。
     */
    private void appendCallbacks(List<ToolCallback> merged,
                                 Set<String> names,
                                 ToolCallback[] callbacks) {
        if (callbacks == null) {
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
