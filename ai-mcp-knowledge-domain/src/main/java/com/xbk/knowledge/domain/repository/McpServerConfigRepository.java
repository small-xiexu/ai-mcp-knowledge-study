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
     * @param query 名称查询条件
     * @return MCP Server 配置
     */
    Optional<McpServerConfig> findByName(McpServerNameQuery query);

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * @param query ID 查询条件
     * @return MCP Server 配置
     */
    Optional<McpServerConfig> findById(IdQuery query);

    /**
     * 保存 MCP Server 配置（新增或更新）
     *
     * @param config MCP Server 配置
     * @return 保存后的配置
     */
    McpServerConfig save(McpServerConfig config);

    /**
     * 判断配置是否存在
     *
     * @param query ID 查询条件
     * @return 是否存在
     */
    boolean existsById(IdQuery query);

    /**
     * 删除配置
     *
     * @param query ID 查询条件
     */
    void deleteById(IdQuery query);

    /**
     * 查询配置分页数据
     *
     * @param query 分页查询条件
     * @return 配置列表
     */
    List<McpServerConfig> findPage(McpServerConfigPageQuery query);

    /**
     * 查询启用的 MCP Server 配置
     *
     * @param query 启用状态查询条件
     * @return 配置列表
     */
    List<McpServerConfig> findByEnabled(EnabledQuery query);

    /**
     * 统计配置总数
     *
     * @return 总数
     */
    long countAll();
}
