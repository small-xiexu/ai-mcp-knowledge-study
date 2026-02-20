package com.xbk.knowledge.config.mcp;

import com.xbk.knowledge.application.service.app.McpServerConfigAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * MCP Server 运行时启动器
 * 启动时加载启用的 MCP Server 配置并注册运行时连接
 *
 * 职责：应用装配配置，用于初始化运行时状态
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpServerRuntimeBootstrap implements ApplicationRunner {

    private final McpServerConfigAppService mcpServerConfigAppService;

    /**
     * 应用启动完成后刷新 MCP Server 运行时连接
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            mcpServerConfigAppService.refreshEnabledServers();
            log.info("MCP Server 运行时初始化完成");
        } catch (Exception e) {
            log.warn("MCP Server 运行时初始化失败", e);
        }
    }
}
