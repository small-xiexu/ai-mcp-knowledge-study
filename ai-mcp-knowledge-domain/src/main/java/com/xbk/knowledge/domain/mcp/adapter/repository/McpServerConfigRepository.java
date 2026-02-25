package com.xbk.knowledge.domain.mcp.adapter.repository;

import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerNameQuery;

import java.util.List;
import java.util.Optional;

/**
 * MCP Server 配置仓储接口
 * 通过仓储抽象隔离数据访问实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author sxie
 */
public interface McpServerConfigRepository {

    /**
     * 根据名称查询 MCP Server 配置
     *
     * 名称用于唯一性校验与定位配置
     * 
     * @param query MCP Server 名称查询条件。
     * @return 可选的MCP 服务配置。
     */
    Optional<McpServerConfig> findByName(McpServerNameQuery query);

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * 按唯一 ID 获取配置
     * 
     * @param query 主键查询条件。
     * @return 可选的MCP 服务配置。
     */
    Optional<McpServerConfig> findById(IdQuery query);

    /**
     * 保存 MCP Server 配置（新增或更新）
     *
     * 统一新增与更新入口
     * 
     * @param config 配置信息。
     * @return 保存后的 MCP Server 配置。
     */
    McpServerConfig save(McpServerConfig config);

    /**
     * 判断配置是否存在
     *
     * 更新/删除前校验存在性
     * 
     * @param query 主键查询条件。
     * @return `true` 表示配置存在，`false` 表示不存在。
     */
    boolean existsById(IdQuery query);

    /**
     * 删除配置
     *
     * 移除无效配置
     * 
     * @param query 主键查询条件。
     */
    void deleteById(IdQuery query);

    /**
     * 查询配置分页数据
     *
     * 分页展示配置列表
     * 
     * @param query 分页查询条件。
     * @return MCP 服务配置列表。
     */
    List<McpServerConfig> findPage(McpServerConfigPageQuery query);

    /**
     * 查询启用的 MCP Server 配置
     *
     * 运行时只加载启用配置
     * 
     * @param query 启用状态查询条件。
     * @return MCP 服务配置列表。
     */
    List<McpServerConfig> findByEnabled(EnabledQuery query);

    /**
     * 统计配置总数
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    long countAll();
}
