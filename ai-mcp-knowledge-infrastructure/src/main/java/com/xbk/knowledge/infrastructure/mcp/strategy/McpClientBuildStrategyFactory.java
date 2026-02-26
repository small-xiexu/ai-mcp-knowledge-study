package com.xbk.knowledge.infrastructure.mcp.strategy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * MCP 客户端构建策略工厂
 *
 * 职责：按策略类型路由并返回对应构建策略
 *
 * @author sxie
 */
@Component
public class McpClientBuildStrategyFactory {

    /**
     * 策略类型与构建策略映射表。
     */
    private final Map<McpClientBuildStrategyType, McpClientBuildStrategy> strategyMap;

    /**
     * 创建策略工厂。
     *
     * @param strategyList 策略列表。
     */
    public McpClientBuildStrategyFactory(List<McpClientBuildStrategy> strategyList) {
        EnumMap<McpClientBuildStrategyType, McpClientBuildStrategy> copiedMap =
                new EnumMap<>(McpClientBuildStrategyType.class);
        if (strategyList != null) {
            for (McpClientBuildStrategy strategy : strategyList) {
                if (strategy == null) {
                    continue;
                }
                McpClientBuildStrategyType strategyType = strategy.getType();
                if (strategyType == null) {
                    throw new IllegalArgumentException("MCP 客户端构建策略类型不能为空");
                }
                if (copiedMap.containsKey(strategyType)) {
                    throw new IllegalArgumentException("重复注册 MCP 客户端构建策略类型: " + strategyType);
                }
                copiedMap.put(strategyType, strategy);
            }
        }
        this.strategyMap = Collections.unmodifiableMap(copiedMap);
    }

    /**
     * 按策略类型获取构建策略。
     *
     * @param strategyType 策略类型。
     * @return MCP 客户端构建策略。
     */
    public McpClientBuildStrategy getStrategy(McpClientBuildStrategyType strategyType) {
        McpClientBuildStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("未注册的 MCP 客户端构建策略类型: " + strategyType);
        }
        return strategy;
    }
}
