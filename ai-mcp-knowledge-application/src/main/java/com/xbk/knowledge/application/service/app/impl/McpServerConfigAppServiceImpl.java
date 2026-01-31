package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.McpServerConfigAppService;
import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.service.IMcpServerConfigService;
import com.xbk.knowledge.application.service.runtime.McpServerRuntimeService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MCP Server 配置应用服务实现
 * 负责 MCP Server 配置相关用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力与运行时注册
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class McpServerConfigAppServiceImpl implements McpServerConfigAppService {

    private final IMcpServerConfigService mcpServerConfigService;
    private final McpServerRuntimeService mcpServerRuntimeService;

    /**
     * 分页查询 MCP Server 配置
     * 负责应用层用例编排，调用领域服务获取分页结果
     */
    @Override
    public PageResult<McpServerConfig> queryMcpServerConfigPage(McpServerConfigPageQuery query) {
        return mcpServerConfigService.queryMcpServerConfigPage(query);
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     * 负责应用层用例编排，调用领域服务获取详情
     */
    @Override
    public McpServerConfig queryMcpServerConfigById(IdQuery query) {
        return mcpServerConfigService.queryMcpServerConfigById(query);
    }

    /**
     * 创建 MCP Server 配置
     * 负责应用层事务边界编排并触发运行时注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig createMcpServerConfig(McpServerConfig config) {
        McpServerConfig savedConfig = mcpServerConfigService.createMcpServerConfig(config);
        if (Boolean.TRUE.equals(savedConfig.getEnabled())) {
            mcpServerRuntimeService.registerOrUpdate(savedConfig);
        }
        return savedConfig;
    }

    /**
     * 更新 MCP Server 配置
     * 负责应用层事务边界编排并触发运行时刷新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig updateMcpServerConfig(McpServerConfig config) {
        McpServerConfig savedConfig = mcpServerConfigService.updateMcpServerConfig(config);
        if (Boolean.TRUE.equals(savedConfig.getEnabled())) {
            mcpServerRuntimeService.registerOrUpdate(savedConfig);
        } else {
            Long id = savedConfig.getId();
            mcpServerRuntimeService.unregister(id);
        }
        return savedConfig;
    }

    /**
     * 删除 MCP Server 配置
     * 负责应用层事务边界编排并释放运行时资源
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMcpServerConfig(IdQuery query) {
        McpServerConfig existing = mcpServerConfigService.queryMcpServerConfigById(query);
        mcpServerConfigService.deleteMcpServerConfig(query);
        if (existing != null && existing.getId() != null) {
            mcpServerRuntimeService.unregister(existing.getId());
        }
    }

    /**
     * 启用 MCP Server
     * 负责应用层事务边界编排并触发运行时注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig enableMcpServer(IdQuery query) {
        McpServerConfig savedConfig = mcpServerConfigService.enableMcpServer(query);
        if (savedConfig != null) {
            mcpServerRuntimeService.registerOrUpdate(savedConfig);
        }
        return savedConfig;
    }

    /**
     * 禁用 MCP Server
     * 负责应用层事务边界编排并释放运行时资源
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig disableMcpServer(IdQuery query) {
        McpServerConfig savedConfig = mcpServerConfigService.disableMcpServer(query);
        if (savedConfig != null && savedConfig.getId() != null) {
            mcpServerRuntimeService.unregister(savedConfig.getId());
        }
        return savedConfig;
    }

    /**
     * 查询启用的 MCP Server
     * 负责应用层用例编排，调用领域服务返回启用列表
     */
    @Override
    public List<McpServerConfig> queryEnabledServers(EnabledQuery query) {
        return mcpServerConfigService.queryEnabledServers(query);
    }

    /**
     * 刷新启用的 MCP Server 运行时连接
     * 用于手动触发全量刷新
     */
    @Override
    public void refreshEnabledServers() {
        List<McpServerConfig> enabledConfigs = mcpServerConfigService.queryEnabledServers(new EnabledQuery(true));
        mcpServerRuntimeService.refresh(enabledConfigs);
    }
}
