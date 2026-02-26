package com.xbk.knowledge.infrastructure.mcp.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.stereotype.Component;

/**
 * WEBSOCKET MCP 客户端构建策略
 *
 * 职责：定义 WEBSOCKET 模式的构建入口（当前版本未实现）
 *
 * @author sxie
 */
@Component
public class WebsocketMcpClientBuildStrategy extends AbstractMcpClientBuildStrategy {

    /**
     * 创建 WEBSOCKET MCP 客户端构建策略。
     *
     * @param objectMapper JSON 序列化器。
     */
    public WebsocketMcpClientBuildStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 获取策略类型。
     *
     * @return WEBSOCKET 策略类型。
     */
    @Override
    public McpClientBuildStrategyType getType() {
        return McpClientBuildStrategyType.WEBSOCKET;
    }

    /**
     * 根据 WEBSOCKET 配置构建同步客户端。
     *
     * @param config MCP 服务配置。
     * @return MCP 同步客户端。
     */
    @Override
    public McpSyncClient build(McpServerConfig config) {
        throw new IllegalArgumentException("当前版本暂不支持 WEBSOCKET 类型");
    }
}
