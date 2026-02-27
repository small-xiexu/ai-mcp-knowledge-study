package com.xbk.knowledge.config.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StringUtils;

/**
 * 惰性 ToolCallbackResolver 配置。
 * 避免在容器启动阶段预先展开 ToolCallbackProvider，改为按工具名解析时再访问 Provider。
 *
 * @author sxie
 */
@Configuration
public class LazyToolCallbackResolverConfig {

    /**
     * MCP 内置 Provider 类型名（与 Spring AI 默认逻辑保持一致，避免重复聚合）。
     */
    private static final String MCP_SYNC_PROVIDER_CLASS = "org.springframework.ai.mcp.SyncMcpToolCallbackProvider";

    /**
     * MCP 内置 Provider 类型名（异步）。
     */
    private static final String MCP_ASYNC_PROVIDER_CLASS = "org.springframework.ai.mcp.AsyncMcpToolCallbackProvider";

    /**
     * 注册惰性 ToolCallbackResolver，覆盖 Spring AI 默认实现。
     *
     * @param applicationContext Spring 上下文。
     * @param toolCallbacks ToolCallback Bean 提供器。
     * @param toolCallbackProviders ToolCallbackProvider Bean 提供器。
     * @return ToolCallbackResolver 实例。
     */
    @Bean
    @Primary
    public ToolCallbackResolver toolCallbackResolver(GenericApplicationContext applicationContext,
                                                     ObjectProvider<ToolCallback> toolCallbacks,
                                                     ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        SpringBeanToolCallbackResolver springBeanResolver = SpringBeanToolCallbackResolver.builder()
                .applicationContext(applicationContext)
                .build();
        return new LazyToolCallbackResolver(toolCallbacks, toolCallbackProviders, springBeanResolver);
    }

    /**
     * 按需解析工具：优先查已注册 ToolCallback，其次查 ToolCallbackProvider，最后回退 Spring Bean 解析。
     */
    private static final class LazyToolCallbackResolver implements ToolCallbackResolver {

        private final ObjectProvider<ToolCallback> toolCallbacks;
        private final ObjectProvider<ToolCallbackProvider> toolCallbackProviders;
        private final SpringBeanToolCallbackResolver springBeanResolver;

        /**
         * 创建惰性工具解析器。
         *
         * @param toolCallbacks 直接注册的 ToolCallback Bean 提供器。
         * @param toolCallbackProviders ToolCallbackProvider Bean 提供器。
         * @param springBeanResolver Spring Bean 回退解析器。
         */
        private LazyToolCallbackResolver(ObjectProvider<ToolCallback> toolCallbacks,
                                         ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
                                         SpringBeanToolCallbackResolver springBeanResolver) {
            this.toolCallbacks = toolCallbacks;
            this.toolCallbackProviders = toolCallbackProviders;
            this.springBeanResolver = springBeanResolver;
        }

        /**
         * 按工具名惰性解析 ToolCallback。
         *
         * 解析顺序：
         * 1. 先从直接注册的 ToolCallback Bean 中匹配；
         * 2. 再遍历普通 ToolCallbackProvider 按需取回 callbacks；
         * 3. 最后回退到 SpringBeanToolCallbackResolver。
         *
         * @param toolName 工具名称。
         * @return 匹配到的 ToolCallback，未命中返回 null。
         */
        @Override
        public ToolCallback resolve(String toolName) {
            if (!StringUtils.hasText(toolName)) {
                return null;
            }

            for (ToolCallback callback : toolCallbacks.orderedStream().toList()) {
                if (hasName(callback, toolName)) {
                    return callback;
                }
            }

            for (ToolCallbackProvider provider : toolCallbackProviders.orderedStream().distinct().toList()) {
                if (provider == null || isMcpToolCallbackProvider(provider.getClass())) {
                    continue;
                }
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks == null) {
                    continue;
                }
                for (ToolCallback callback : callbacks) {
                    if (hasName(callback, toolName)) {
                        return callback;
                    }
                }
            }

            return springBeanResolver.resolve(toolName);
        }

        /**
         * 判断回调定义名是否与目标工具名一致。
         *
         * @param callback 待判断的回调。
         * @param expectedName 目标工具名。
         * @return 命中返回 true，否则返回 false。
         */
        private static boolean hasName(ToolCallback callback, String expectedName) {
            if (callback == null || callback.getToolDefinition() == null) {
                return false;
            }
            return expectedName.equals(callback.getToolDefinition().name());
        }

        /**
         * 判断类型是否属于 Spring AI MCP 内置 Provider。
         *
         * 这些 Provider 在框架自动配置中已有单独处理，若在此再次展开会导致重复聚合；
         * 因此这里通过遍历父类链做类型名匹配并排除。
         *
         * @param type Provider 运行时类型。
         * @return 是 MCP 内置 Provider 返回 true，否则返回 false。
         */
        private static boolean isMcpToolCallbackProvider(Class<?> type) {
            Class<?> current = type;
            while (current != null && current != Object.class) {
                String typeName = current.getName();
                if (MCP_SYNC_PROVIDER_CLASS.equals(typeName) || MCP_ASYNC_PROVIDER_CLASS.equals(typeName)) {
                    return true;
                }
                current = current.getSuperclass();
            }
            return false;
        }
    }
}
