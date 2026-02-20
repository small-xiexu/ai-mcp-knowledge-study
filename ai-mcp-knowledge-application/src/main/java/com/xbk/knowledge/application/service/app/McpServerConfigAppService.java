package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerConfigPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * MCP Server 配置应用服务接口
 * 负责 MCP Server 配置相关用例编排
 *
 * 职责：应用层用例接口，用于封装调用入口
 * @author sxie
 */
public interface McpServerConfigAppService {

    /**
     * 分页查询 MCP Server 配置
     *
     * @param query 分页查询条件
     * @return 分页结果
     */
    PageResult<McpServerConfig> queryMcpServerConfigPage(McpServerConfigPageQuery query);

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * @param query ID 查询条件
     * @return MCP Server 配置
     */
    McpServerConfig queryMcpServerConfigById(IdQuery query);

    /**
     * 创建 MCP Server 配置
     *
     * @param config MCP Server 配置
     * @return 创建后的配置
     */
    McpServerConfig createMcpServerConfig(McpServerConfig config);

    /**
     * 更新 MCP Server 配置
     *
     * @param config MCP Server 配置（必须包含 ID）
     * @return 更新后的配置
     */
    McpServerConfig updateMcpServerConfig(McpServerConfig config);

    /**
     * 删除 MCP Server 配置
     *
     * @param query ID 查询条件
     */
    void deleteMcpServerConfig(IdQuery query);

    /**
     * 启用 MCP Server
     *
     * @param query ID 查询条件
     * @return 更新后的配置
     */
    McpServerConfig enableMcpServer(IdQuery query);

    /**
     * 禁用 MCP Server
     *
     * @param query ID 查询条件
     * @return 更新后的配置
     */
    McpServerConfig disableMcpServer(IdQuery query);

    /**
     * 查询启用的 MCP Server
     *
     * @param query 启用状态查询条件
     * @return MCP Server 列表
     */
    List<McpServerConfig> queryEnabledServers(EnabledQuery query);

    /**
     * 刷新启用的 MCP Server 运行时连接
     */
    void refreshEnabledServers();

    /**
     * 刷新指定 MCP Server 运行时连接
     *
     * @param query ID 查询条件
     */
    void refreshServer(IdQuery query);
}
