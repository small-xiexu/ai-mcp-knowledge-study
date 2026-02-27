package com.xbk.knowledge.infrastructure.mcp;

import com.xbk.knowledge.application.model.dto.McpToolInfo;
import com.xbk.knowledge.application.service.mcp.McpToolCatalogService;
import com.xbk.knowledge.config.McpToolProperties;
import com.xbk.knowledge.types.tool.ToolKeyAware;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MCP 工具目录服务实现
 * 提供可读的工具清单并缓存一定时间
 *
 * @author sxie
 */
@Service
public class McpToolCatalogServiceImpl implements McpToolCatalogService {

    /**
     * 工具提示词标题前缀。
     */
    private static final String TOOL_PROMPT_HEADER = "可用工具列表";

    /**
     * 空工具集缓存秒数。
     */
    private static final int EMPTY_CACHE_SECONDS = 3;

    /**
     * 工具回调提供器。
     */
    private final ToolCallbackProvider toolCallbackProvider;

    /**
     * MCP 工具配置属性。
     */
    private final McpToolProperties properties;

    /**
     * 当前工具快照缓存。
     */
    private volatile ToolSnapshot snapshot;

    /**
     * 创建 MCP 工具目录服务并注入依赖组件。
     * 
     * @param toolCallbackProvider 工具回调提供器。
     * @param properties 配置属性。
     */
    public McpToolCatalogServiceImpl(ToolCallbackProvider toolCallbackProvider,
                                     McpToolProperties properties) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.properties = properties;
    }

    /**
     * 构建工具提示词
     * <p>
     * 减少频繁拼接带来的成本，使用缓存提升性能
     * 
     * @return 工具提示词文本。
     */
    @Override
    public String buildToolPrompt() {
        // 1、无锁快路径：大多数请求直接命中缓存，避免进入同步块。
        ToolSnapshot cached = snapshot;
        long now = Instant.now().toEpochMilli();
        if (cached != null && now < cached.getExpireAt()) {
            return cached.getPrompt();
        }
        // 2、缓存未命中时再加锁：仅允许一个线程执行刷新，避免并发重复构建。
        synchronized (this) {
            // 3、双重检查：等待锁期间可能已有其他线程刷新过缓存。
            ToolSnapshot refreshed = snapshot;
            if (refreshed != null && now < refreshed.getExpireAt()) {
                return refreshed.getPrompt();
            }
            // 4、真正需要刷新时才重建快照并覆盖缓存。
            ToolSnapshot newSnapshot = refreshSnapshot(now);
            snapshot = newSnapshot;
            return newSnapshot.getPrompt();
        }
    }

    /**
     * 列出可用工具
     * <p>
     * 为前端展示与提示词构建提供数据
     * 
     * @return McpToolInfo 列表。
     */
    @Override
    public List<McpToolInfo> listTools() {
        // 从统一 ToolCallbackProvider 拉取当前可见工具集合（已包含底层合并/过滤逻辑）。
        ToolCallback[] callbacks = toolCallbackProvider != null
                ? toolCallbackProvider.getToolCallbacks()
                : new ToolCallback[0];
        if (callbacks.length == 0) {
            return Collections.emptyList();
        }
        List<McpToolInfo> result = new ArrayList<>();
        for (ToolCallback callback : callbacks) {
            // 防御空对象，避免后续访问定义信息触发空指针。
            if (callback == null) {
                continue;
            }
            ToolDefinition definition = callback.getToolDefinition();
            // 工具名是提示词展示与调用绑定的关键字段，缺失则跳过。
            if (!StringUtils.hasText(definition.name())) {
                continue;
            }
            String toolKey = null;
            String source = null;
            // 识别治理包装后的工具，补充 toolKey/source 供可观测与排障使用。
            if (callback instanceof ToolKeyAware aware) {
                toolKey = aware.toolKey();
                source = aware.toolSource();
            }
            // 转为上层展示结构，统一填充描述与输入 schema。
            McpToolInfo info = McpToolInfo.builder()
                    .name(definition.name())
                    .toolKey(toolKey)
                    .source(source)
                    .description(StringUtils.hasText(definition.description()) ? definition.description() : "暂无描述")
                    .inputSchema(definition.inputSchema())
                    .build();
            result.add(info);
        }
        return result;
    }

    /**
     * 刷新缓存快照
     * <p>
     * 统一生成提示词并设置过期时间
     * 
     * @param now 当前时间。
     * @return 工具快照。
     */
    private ToolSnapshot refreshSnapshot(long now) {
        // 获取可用工具
        List<McpToolInfo> tools = listTools();
        List<String> lines = new ArrayList<>();
        if (!CollectionUtils.isEmpty(tools)) {
            for (McpToolInfo tool : tools) {
                if (tool == null || !StringUtils.hasText(tool.getName())) {
                    continue;
                }
                String safeDescription = StringUtils.hasText(tool.getDescription()) ? tool.getDescription() : "暂无描述";
                lines.add("- " + tool.getName() + ": " + safeDescription);
            }
        }
        String prompt = "";
        if (!CollectionUtils.isEmpty(lines)) {
            StringBuilder builder = new StringBuilder();
            builder.append(TOOL_PROMPT_HEADER).append("\n");
            for (String line : lines) {
                builder.append(line).append("\n");
            }
            prompt = builder.toString().trim();
        }
        // 空工具结果不做长缓存，允许快速重试拉取
        int ttlSeconds = CollectionUtils.isEmpty(tools)
                ? Math.min(properties.getCacheSeconds(), EMPTY_CACHE_SECONDS)
                : properties.getCacheSeconds();
        long expireAt = now + ttlSeconds * 1000L;
        return new ToolSnapshot(prompt, expireAt);
    }

    /**
     * 工具提示词缓存快照
     * <p>
     * 避免重复构建提示词
     */
    private static class ToolSnapshot {
        /**
         * 工具提示词文本。
         */
        private final String prompt;

        /**
         * 缓存过期时间戳（毫秒）。
         */
        private final long expireAt;

        private ToolSnapshot(String prompt, long expireAt) {
            this.prompt = prompt;
            this.expireAt = expireAt;
        }

        private String getPrompt() {
            return prompt;
        }

        private long getExpireAt() {
            return expireAt;
        }
    }
}
