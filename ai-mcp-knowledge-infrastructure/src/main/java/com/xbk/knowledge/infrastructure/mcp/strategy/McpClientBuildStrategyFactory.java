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
        // 使用 EnumMap 降低枚举键存取开销，并通过 copiedMap 规避对入参集合的副作用。
        EnumMap<McpClientBuildStrategyType, McpClientBuildStrategy> copiedMap =
                new EnumMap<>(McpClientBuildStrategyType.class);
        if (strategyList != null) {
            for (McpClientBuildStrategy strategy : strategyList) {
                if (strategy == null) {
                    continue;
                }
                McpClientBuildStrategyType strategyType = strategy.getType();
                // 策略类型是工厂路由的唯一索引，必须显式存在。
                if (strategyType == null) {
                    throw new IllegalArgumentException("MCP 客户端构建策略类型不能为空");
                }
                // 相同类型仅允许一个实现，避免运行时路由歧义。
                if (copiedMap.containsKey(strategyType)) {
                    throw new IllegalArgumentException("重复注册 MCP 客户端构建策略类型: " + strategyType);
                }
                copiedMap.put(strategyType, strategy);
            }
        }
        // 对外暴露只读视图，防止工厂初始化后被二次篡改。
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
