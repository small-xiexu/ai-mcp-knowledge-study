package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerNameQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP Server 配置 Mapper
 * 使用 XML 执行 SQL，避免注解或默认方法绕过约束
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 * @author xiexu
 */
@Mapper
public interface McpServerConfigMapper extends BaseMapper<McpServerConfig> {

    /**
     * 新增 MCP Server 配置
     *
     * @param config MCP Server 配置
     * @return 影响行数
     */
    int insertMcpServerConfig(McpServerConfig config);

    /**
     * 更新 MCP Server 配置
     *
     * @param config MCP Server 配置
     * @return 影响行数
     */
    int updateMcpServerConfig(McpServerConfig config);

    /**
     * 删除 MCP Server 配置
     *
     * @param query ID 查询条件
     * @return 影响行数
     */
    int deleteMcpServerConfigById(IdQuery query);

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * @param query ID 查询条件
     * @return MCP Server 配置
     */
    McpServerConfig findById(IdQuery query);

    /**
     * 根据名称查询 MCP Server 配置
     *
     * @param query 名称查询条件
     * @return MCP Server 配置
     */
    McpServerConfig findByName(McpServerNameQuery query);

    /**
     * 根据启用状态查询 MCP Server 配置
     *
     * @param query 启用状态查询条件
     * @return MCP Server 配置列表
     */
    List<McpServerConfig> findByEnabled(EnabledQuery query);

    /**
     * 查询 MCP Server 配置分页数据
     *
     * @param query 分页查询条件
     * @return MCP Server 配置列表
     */
    List<McpServerConfig> findPage(McpServerConfigPageQuery query);

    /**
     * 统计 MCP Server 配置总数
     *
     * @return 总数
     */
    long countAll();
}
