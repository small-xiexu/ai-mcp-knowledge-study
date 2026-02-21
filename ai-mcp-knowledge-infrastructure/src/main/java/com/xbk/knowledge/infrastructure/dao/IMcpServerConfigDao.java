package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpServerConfigPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerNameQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP Server 配置 Mapper
 * 使用 XML 执行 SQL，避免注解或默认方法绕过约束
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 * @author sxie
 */
@Mapper
public interface IMcpServerConfigDao extends BaseMapper<McpServerConfigPO> {

    /**
     * 新增 MCP Server 配置
     *
     * 为什么：落库 MCP 配置
     * 入参：MCP Server 配置
     * 出参：影响行数
     */
    int insertMcpServerConfig(McpServerConfigPO config);

    /**
     * 更新 MCP Server 配置
     *
     * 为什么：更新 MCP 配置字段
     * 入参：MCP Server 配置
     * 出参：影响行数
     */
    int updateMcpServerConfig(McpServerConfigPO config);

    /**
     * 删除 MCP Server 配置
     *
     * 为什么：清理无效配置
     * 入参：ID 查询条件
     * 出参：影响行数
     */
    int deleteMcpServerConfigById(IdQuery query);

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * 为什么：按唯一 ID 获取配置
     * 入参：ID 查询条件
     * 出参：MCP Server 配置
     */
    McpServerConfigPO findById(IdQuery query);

    /**
     * 根据名称查询 MCP Server 配置
     *
     * 为什么：名称用于唯一性校验
     * 入参：名称查询条件
     * 出参：MCP Server 配置
     */
    McpServerConfigPO findByName(McpServerNameQuery query);

    /**
     * 根据启用状态查询 MCP Server 配置
     *
     * 为什么：运行时只加载启用配置
     * 入参：启用状态查询条件
     * 出参：MCP Server 配置列表
     */
    List<McpServerConfigPO> findByEnabled(EnabledQuery query);

    /**
     * 查询 MCP Server 配置分页数据
     *
     * 为什么：控制单次返回数量
     * 入参：分页查询条件
     * 出参：MCP Server 配置列表
     */
    List<McpServerConfigPO> findPage(McpServerConfigPageQuery query);

    /**
     * 统计 MCP Server 配置总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    long countAll();

}
