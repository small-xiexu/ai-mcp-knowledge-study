package com.xbk.knowledge.infrastructure.mcp.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * STDIO MCP 客户端构建策略
 *
 * 职责：根据 STDIO 配置构建 MCP 同步客户端
 *
 * @author sxie
 */
@Component
public class StdioMcpClientBuildStrategy extends AbstractMcpClientBuildStrategy {

    /**
     * 创建 STDIO MCP 客户端构建策略。
     *
     * @param objectMapper JSON 序列化器。
     */
    public StdioMcpClientBuildStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 获取策略类型。
     *
     * @return STDIO 策略类型。
     */
    @Override
    public McpClientBuildStrategyType getType() {
        return McpClientBuildStrategyType.STDIO;
    }

    /**
     * 根据 STDIO 配置构建同步客户端。
     *
     * @param config MCP 服务配置。
     * @return MCP 同步客户端。
     */
    @Override
    public McpSyncClient build(McpServerConfig config) {
        String command = config.getCommand();
        if (!StringUtils.hasText(command)) {
            throw new IllegalArgumentException("STDIO 模式必须配置 command");
        }

        List<String> args = parseStringList(config.getArgs());
        Map<String, String> env = parseStringMap(config.getEnv());
        ServerParameters parameters = ServerParameters
                .builder(command)
                .args(args)
                .env(env)
                .build();
        StdioClientTransport transport = new StdioClientTransport(parameters, getMcpJsonMapper());
        return buildSyncClient(transport, config);
    }
}
