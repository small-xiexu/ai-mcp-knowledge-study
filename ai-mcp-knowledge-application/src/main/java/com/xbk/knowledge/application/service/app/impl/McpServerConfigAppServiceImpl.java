package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.McpServerConfigAppService;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.mcp.service.IMcpServerConfigService;
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
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class McpServerConfigAppServiceImpl implements McpServerConfigAppService {
    /**
     * MCP Server 配置领域服务，用于配置项读写与启停。
     */
    private final IMcpServerConfigService mcpServerConfigService;

    /**
     * MCP Server 运行时服务，用于注册、更新和注销运行时连接。
     */
    private final McpServerRuntimeService mcpServerRuntimeService;

    /**
     * 分页查询 MCP Server 配置
     *
     * 统一查询入口，隔离应用层与领域层协议
     * 
     * @param query 分页查询条件。
     * @return MCP 服务配置分页结果。
     */
    @Override
    public PageResult<McpServerConfig> queryMcpServerConfigPage(McpServerConfigPageQuery query) {
        return mcpServerConfigService.queryMcpServerConfigPage(query);
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * 统一详情查询入口，便于后续扩展校验
     * 
     * @param query 主键查询条件。
     * @return MCP Server 配置详情。
     */
    @Override
    public McpServerConfig queryMcpServerConfigById(IdQuery query) {
        return mcpServerConfigService.queryMcpServerConfigById(query);
    }

    /**
     * 创建 MCP Server 配置
     *
     * 创建后由用户手动触发运行时刷新，避免初始化失败导致保存回滚
     * 
     * @param config 配置信息。
     * @return 创建后的 MCP Server 配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig createMcpServerConfig(McpServerConfig config) {
        return mcpServerConfigService.createMcpServerConfig(config);
    }

    /**
     * 更新 MCP Server 配置
     *
     * 配置变更后由用户手动触发运行时刷新，避免初始化失败导致保存回滚
     * 
     * @param config 配置信息。
     * @return 更新后的 MCP Server 配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig updateMcpServerConfig(McpServerConfig config) {
        McpServerConfig savedConfig = mcpServerConfigService.updateMcpServerConfig(config);
        if (!Boolean.TRUE.equals(savedConfig.getEnabled())) {
            Long id = savedConfig.getId();
            // 禁用后释放运行时资源
            mcpServerRuntimeService.unregister(id);
        }
        return savedConfig;
    }

    /**
     * 删除 MCP Server 配置
     *
     * 删除配置需同步清理运行时连接，避免悬挂实例
     * 
     * @param query 主键查询条件。
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
     *
     * 启用后由用户手动触发运行时刷新，避免初始化失败导致启用回滚
     * 
     * @param query 主键查询条件。
     * @return 启用后的 MCP Server 配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig enableMcpServer(IdQuery query) {
        return mcpServerConfigService.enableMcpServer(query);
    }

    /**
     * 禁用 MCP Server
     *
     * 禁用后需释放运行时连接
     * 
     * @param query 主键查询条件。
     * @return 禁用后的 MCP Server 配置。
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
     *
     * 运行时只需处理启用的配置
     * 
     * @param query 启用状态查询条件。
     * @return MCP 服务配置列表。
     */
    @Override
    public List<McpServerConfig> queryEnabledServers(EnabledQuery query) {
        return mcpServerConfigService.queryEnabledServers(query);
    }

    /**
     * 刷新启用的 MCP Server 运行时连接
     *
     * 支持手动触发全量刷新，确保配置生效
     */
    @Override
    public void refreshEnabledServers() {
        List<McpServerConfig> enabledConfigs = mcpServerConfigService.queryEnabledServers(new EnabledQuery(true));
        mcpServerRuntimeService.refresh(enabledConfigs);
    }

    /**
     * 刷新指定 MCP Server 运行时连接
     *
     * 单条配置变更时避免全量刷新影响其它连接
     * 
     * @param query 主键查询条件。
     */
    @Override
    public void refreshServer(IdQuery query) {
        McpServerConfig config = mcpServerConfigService.queryMcpServerConfigById(query);
        if (config == null || config.getId() == null) {
            return;
        }
        if (Boolean.TRUE.equals(config.getEnabled())) {
            // 单条刷新重建运行时连接
            mcpServerRuntimeService.registerOrUpdate(config);
        } else {
            // 禁用配置不应维持运行时连接
            mcpServerRuntimeService.unregister(config.getId());
        }
    }
}
