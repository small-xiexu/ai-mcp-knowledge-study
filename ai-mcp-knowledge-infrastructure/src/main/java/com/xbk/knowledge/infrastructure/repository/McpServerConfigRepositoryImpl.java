package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerNameQuery;
import com.xbk.knowledge.domain.repository.McpServerConfigRepository;
import com.xbk.knowledge.infrastructure.mapper.McpServerConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * MCP Server 配置仓储实现
 * 通过 Mapper 执行 XML SQL，隔离持久化细节
 *
 * 职责：仓储实现，用于落地数据访问
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class McpServerConfigRepositoryImpl implements McpServerConfigRepository {

    private final McpServerConfigMapper mcpServerConfigMapper;

    /**
     * 根据名称查询 MCP Server 配置
     * 用于唯一性校验与快速定位
     *
     * 为什么：名称用于唯一性校验
     * 入参：名称查询条件
     * 出参：配置
     */
    @Override
    public Optional<McpServerConfig> findByName(McpServerNameQuery query) {
        if (query == null || query.getServerName() == null) {
            return Optional.empty();
        }
        McpServerConfig config = mcpServerConfigMapper.findByName(query);
        return Optional.ofNullable(config);
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     * 用于详情展示与编辑加载
     *
     * 为什么：按唯一 ID 定位配置
     * 入参：ID 查询条件
     * 出参：配置
     */
    @Override
    public Optional<McpServerConfig> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        McpServerConfig config = mcpServerConfigMapper.findById(query);
        return Optional.ofNullable(config);
    }

    /**
     * 保存 MCP Server 配置
     * 统一插入与更新逻辑，保证数据一致性
     *
     * 为什么：统一新增与更新入口
     * 入参：配置实体
     * 出参：保存后的配置
     */
    @Override
    public McpServerConfig save(McpServerConfig config) {
        if (config == null) {
            return null;
        }
        if (config.getId() == null) {
            mcpServerConfigMapper.insertMcpServerConfig(config);
            return config;
        }
        mcpServerConfigMapper.updateMcpServerConfig(config);
        return config;
    }

    /**
     * 判断 MCP Server 配置是否存在
     * 用于删除与更新前置校验
     *
     * 为什么：避免更新/删除不存在的数据
     * 入参：ID 查询条件
     * 出参：是否存在
     */
    @Override
    public boolean existsById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return false;
        }
        return mcpServerConfigMapper.findById(query) != null;
    }

    /**
     * 删除 MCP Server 配置
     * 用于配置管理删除操作
     *
     * 为什么：清理无效配置
     * 入参：ID 查询条件
     * 出参：无
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        mcpServerConfigMapper.deleteMcpServerConfigById(query);
    }

    /**
     * 查询 MCP Server 配置分页数据
     * 用于配置管理分页展示
     *
     * 为什么：控制单次返回数量
     * 入参：分页查询条件
     * 出参：配置列表
     */
    @Override
    public List<McpServerConfig> findPage(McpServerConfigPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return mcpServerConfigMapper.findPage(query);
    }

    /**
     * 查询启用的 MCP Server 配置
     * 用于运行时注册加载
     *
     * 为什么：运行时只加载启用配置
     * 入参：启用状态查询条件
     * 出参：配置列表
     */
    @Override
    public List<McpServerConfig> findByEnabled(EnabledQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return mcpServerConfigMapper.findByEnabled(query);
    }

    /**
     * 统计 MCP Server 配置总数
     * 用于分页统计
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    @Override
    public long countAll() {
        return mcpServerConfigMapper.countAll();
    }
}
