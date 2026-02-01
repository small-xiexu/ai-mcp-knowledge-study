package com.xbk.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MCP 工具配置
 * 控制工具清单缓存刷新策略
 *
 * @author xiexu
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp.tools")
public class McpToolProperties {

    /**
     * 工具清单缓存秒数
     */
    private int cacheSeconds = 60;
}
