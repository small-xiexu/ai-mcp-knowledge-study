package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.McpServerConfigAppService;
import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.service.mcp.IMcpServerConfigService;
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
     *
     * 为什么：统一查询入口，隔离应用层与领域层协议
     * 入参：分页查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<McpServerConfig> queryMcpServerConfigPage(McpServerConfigPageQuery query) {
        return mcpServerConfigService.queryMcpServerConfigPage(query);
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * 为什么：统一详情查询入口，便于后续扩展校验
     * 入参：ID 查询对象
     * 出参：配置详情
     */
    @Override
    public McpServerConfig queryMcpServerConfigById(IdQuery query) {
        return mcpServerConfigService.queryMcpServerConfigById(query);
    }

    /**
     * 创建 MCP Server 配置
     *
     * 为什么：创建后由用户手动触发运行时刷新，避免初始化失败导致保存回滚
     * 入参：配置实体
     * 出参：持久化后的配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig createMcpServerConfig(McpServerConfig config) {
        return mcpServerConfigService.createMcpServerConfig(config);
    }

    /**
     * 更新 MCP Server 配置
     *
     * 为什么：配置变更后由用户手动触发运行时刷新，避免初始化失败导致保存回滚
     * 入参：配置实体
     * 出参：更新后的配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig updateMcpServerConfig(McpServerConfig config) {
        McpServerConfig savedConfig = mcpServerConfigService.updateMcpServerConfig(config);
        if (!Boolean.TRUE.equals(savedConfig.getEnabled())) {
            Long id = savedConfig.getId();
            /*
             * 目的：禁用后释放运行时资源
             */
            mcpServerRuntimeService.unregister(id);
        }
        return savedConfig;
    }

    /**
     * 删除 MCP Server 配置
     *
     * 为什么：删除配置需同步清理运行时连接，避免悬挂实例
     * 入参：ID 查询对象
     * 出参：无
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
     * 为什么：启用后由用户手动触发运行时刷新，避免初始化失败导致启用回滚
     * 入参：ID 查询对象
     * 出参：启用后的配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServerConfig enableMcpServer(IdQuery query) {
        return mcpServerConfigService.enableMcpServer(query);
    }

    /**
     * 禁用 MCP Server
     *
     * 为什么：禁用后需释放运行时连接
     * 入参：ID 查询对象
     * 出参：禁用后的配置
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
     * 为什么：运行时只需处理启用的配置
     * 入参：启用状态查询对象
     * 出参：启用配置列表
     */
    @Override
    public List<McpServerConfig> queryEnabledServers(EnabledQuery query) {
        return mcpServerConfigService.queryEnabledServers(query);
    }

    /**
     * 刷新启用的 MCP Server 运行时连接
     *
     * 为什么：支持手动触发全量刷新，确保配置生效
     * 入参：无
     * 出参：无
     */
    @Override
    public void refreshEnabledServers() {
        List<McpServerConfig> enabledConfigs = mcpServerConfigService.queryEnabledServers(new EnabledQuery(true));
        mcpServerRuntimeService.refresh(enabledConfigs);
    }

    /**
     * 刷新指定 MCP Server 运行时连接
     *
     * 为什么：单条配置变更时避免全量刷新影响其它连接
     * 入参：ID 查询对象
     * 出参：无
     */
    @Override
    public void refreshServer(IdQuery query) {
        McpServerConfig config = mcpServerConfigService.queryMcpServerConfigById(query);
        if (config == null || config.getId() == null) {
            return;
        }
        if (Boolean.TRUE.equals(config.getEnabled())) {
            /*
             * 目的：单条刷新重建运行时连接
             */
            mcpServerRuntimeService.registerOrUpdate(config);
        } else {
            /*
             * 目的：禁用配置不应维持运行时连接
             */
            mcpServerRuntimeService.unregister(config.getId());
        }
    }
}
