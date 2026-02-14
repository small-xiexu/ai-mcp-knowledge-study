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
 * @author xiexu
 */
@Service
public class McpToolCatalogServiceImpl implements McpToolCatalogService {

    private static final String TOOL_PROMPT_HEADER = "可用工具列表：";
    private static final int EMPTY_CACHE_SECONDS = 3;

    private final ToolCallbackProvider toolCallbackProvider;
    private final McpToolProperties properties;
    private volatile ToolSnapshot snapshot;

    public McpToolCatalogServiceImpl(ToolCallbackProvider toolCallbackProvider,
                                     McpToolProperties properties) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.properties = properties;
    }

    /**
     * 构建工具提示词
     *
     * 为什么：减少频繁拼接带来的成本，使用缓存提升性能
     * 入参：无
     * 出参：工具提示词
     */
    @Override
    public String buildToolPrompt() {
        ToolSnapshot cached = snapshot;
        long now = Instant.now().toEpochMilli();
        if (cached != null && now < cached.getExpireAt()) {
            return cached.getPrompt();
        }
        synchronized (this) {
            ToolSnapshot refreshed = snapshot;
            if (refreshed != null && now < refreshed.getExpireAt()) {
                return refreshed.getPrompt();
            }
            ToolSnapshot newSnapshot = refreshSnapshot(now);
            snapshot = newSnapshot;
            return newSnapshot.getPrompt();
        }
    }

    /**
     * 列出可用工具
     *
     * 为什么：为前端展示与提示词构建提供数据
     * 入参：无
     * 出参：工具列表
     */
    @Override
    public List<McpToolInfo> listTools() {
        ToolCallback[] callbacks = toolCallbackProvider != null
                ? toolCallbackProvider.getToolCallbacks()
                : new ToolCallback[0];
        if (callbacks == null || callbacks.length == 0) {
            return Collections.emptyList();
        }
        List<McpToolInfo> result = new ArrayList<>();
        for (ToolCallback callback : callbacks) {
            if (callback == null) {
                continue;
            }
            ToolDefinition definition = callback.getToolDefinition();
            if (definition == null || !StringUtils.hasText(definition.name())) {
                continue;
            }
            String toolKey = null;
            String source = null;
            if (callback instanceof ToolKeyAware aware) {
                toolKey = aware.toolKey();
                source = aware.toolSource();
            }
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
     *
     * 为什么：统一生成提示词并设置过期时间
     */
    private ToolSnapshot refreshSnapshot(long now) {
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
        /*
         * 目的：空工具结果不做长缓存，允许快速重试拉取
         */
        int ttlSeconds = CollectionUtils.isEmpty(tools)
                ? Math.min(properties.getCacheSeconds(), EMPTY_CACHE_SECONDS)
                : properties.getCacheSeconds();
        long expireAt = now + ttlSeconds * 1000L;
        return new ToolSnapshot(prompt, expireAt);
    }

    /**
     * 工具提示词缓存快照
     *
     * 为什么：避免重复构建提示词
     */
    private static class ToolSnapshot {
        private final String prompt;
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
