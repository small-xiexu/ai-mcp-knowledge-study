package com.xbk.knowledge.domain.mcp.service;

import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerConfigPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * MCP Server 配置领域服务接口
 * 定义 MCP Server 配置相关领域能力
 *
 * 职责：领域服务接口，用于封装业务规则
 * @author sxie
 */
public interface IMcpServerConfigService {

    /**
     * 分页查询 MCP Server 配置
     *
     * 统一分页查询能力入口
     * 
     * @param query 分页查询条件。
     * @return MCP 服务配置分页结果。
     */
    PageResult<McpServerConfig> queryMcpServerConfigPage(McpServerConfigPageQuery query);

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * 按唯一 ID 获取配置
     * 
     * @param query 主键查询条件。
     * @return MCP Server 配置详情。
     */
    McpServerConfig queryMcpServerConfigById(IdQuery query);

    /**
     * 创建 MCP Server 配置
     *
     * 统一创建入口以保障规则一致
     * 
     * @param config 配置信息。
     * @return 创建后的 MCP Server 配置。
     */
    McpServerConfig createMcpServerConfig(McpServerConfig config);

    /**
     * 更新 MCP Server 配置
     *
     * 统一更新入口以保障规则一致
     * 
     * @param config 配置信息。
     * @return 更新后的 MCP Server 配置。
     */
    McpServerConfig updateMcpServerConfig(McpServerConfig config);

    /**
     * 删除 MCP Server 配置
     *
     * 统一删除入口以保障规则一致
     * 
     * @param query 主键查询条件。
     */
    void deleteMcpServerConfig(IdQuery query);

    /**
     * 启用 MCP Server
     *
     * 统一启用入口以保障规则一致
     * 
     * @param query 主键查询条件。
     * @return 启用后的 MCP Server 配置。
     */
    McpServerConfig enableMcpServer(IdQuery query);

    /**
     * 禁用 MCP Server
     *
     * 统一禁用入口以保障规则一致
     * 
     * @param query 主键查询条件。
     * @return 禁用后的 MCP Server 配置。
     */
    McpServerConfig disableMcpServer(IdQuery query);

    /**
     * 查询启用的 MCP Server
     *
     * 运行时只加载启用配置
     * 
     * @param query 启用状态查询条件。
     * @return MCP 服务配置列表。
     */
    List<McpServerConfig> queryEnabledServers(EnabledQuery query);
}
