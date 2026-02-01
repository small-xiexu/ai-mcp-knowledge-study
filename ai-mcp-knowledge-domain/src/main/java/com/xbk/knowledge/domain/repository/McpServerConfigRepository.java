package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerNameQuery;

import java.util.List;
import java.util.Optional;

/**
 * MCP Server 配置仓储接口
 * 通过仓储抽象隔离数据访问实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author xiexu
 */
public interface McpServerConfigRepository {

    /**
     * 根据名称查询 MCP Server 配置
     *
     * 为什么：名称用于唯一性校验与定位配置
     * 入参：名称查询条件
     * 出参：MCP Server 配置
     */
    Optional<McpServerConfig> findByName(McpServerNameQuery query);

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * 为什么：按唯一 ID 获取配置
     * 入参：ID 查询条件
     * 出参：MCP Server 配置
     */
    Optional<McpServerConfig> findById(IdQuery query);

    /**
     * 保存 MCP Server 配置（新增或更新）
     *
     * 为什么：统一新增与更新入口
     * 入参：MCP Server 配置
     * 出参：保存后的配置
     */
    McpServerConfig save(McpServerConfig config);

    /**
     * 判断配置是否存在
     *
     * 为什么：更新/删除前校验存在性
     * 入参：ID 查询条件
     * 出参：是否存在
     */
    boolean existsById(IdQuery query);

    /**
     * 删除配置
     *
     * 为什么：移除无效配置
     * 入参：ID 查询条件
     * 出参：无
     */
    void deleteById(IdQuery query);

    /**
     * 查询配置分页数据
     *
     * 为什么：分页展示配置列表
     * 入参：分页查询条件
     * 出参：配置列表
     */
    List<McpServerConfig> findPage(McpServerConfigPageQuery query);

    /**
     * 查询启用的 MCP Server 配置
     *
     * 为什么：运行时只加载启用配置
     * 入参：启用状态查询条件
     * 出参：配置列表
     */
    List<McpServerConfig> findByEnabled(EnabledQuery query);

    /**
     * 统计配置总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    long countAll();
}
