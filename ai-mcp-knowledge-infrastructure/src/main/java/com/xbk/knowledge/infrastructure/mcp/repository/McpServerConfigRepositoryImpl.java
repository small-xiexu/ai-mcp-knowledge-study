package com.xbk.knowledge.infrastructure.mcp.repository;

import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerNameQuery;
import com.xbk.knowledge.domain.mcp.adapter.repository.McpServerConfigRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IMcpServerConfigDao;
import com.xbk.knowledge.infrastructure.dao.po.McpServerConfigPO;
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
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpServerConfigRepositoryImpl implements McpServerConfigRepository {

    /**
     * MCP Server 配置数据访问对象。
     */
    private final IMcpServerConfigDao mcpServerConfigMapper;

    /**
     * 根据名称查询 MCP Server 配置
     * 用于唯一性校验与快速定位
     *
     * 名称用于唯一性校验
     * 
     * @param query MCP Server 名称查询条件。
     * @return 可选的MCP 服务配置。
     */
    @Override
    public Optional<McpServerConfig> findByName(McpServerNameQuery query) {
        if (query == null || query.getServerName() == null) {
            return Optional.empty();
        }
        McpServerConfig config = BeanMappingUtils.map(mcpServerConfigMapper.findByName(query), McpServerConfig.class);
        return Optional.ofNullable(config);
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     * 用于详情展示与编辑加载
     *
     * 按唯一 ID 定位配置
     * 
     * @param query 主键查询条件。
     * @return 可选的MCP 服务配置。
     */
    @Override
    public Optional<McpServerConfig> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        McpServerConfig config = BeanMappingUtils.map(mcpServerConfigMapper.findById(query), McpServerConfig.class);
        return Optional.ofNullable(config);
    }

    /**
     * 保存 MCP Server 配置
     * 统一插入与更新逻辑，保证数据一致性
     *
     * 统一新增与更新入口
     * 
     * @param config 配置信息。
     * @return 保存后的 MCP Server 配置。
     */
    @Override
    public McpServerConfig save(McpServerConfig config) {
        if (config == null) {
            return null;
        }
        if (config.getId() == null) {
            mcpServerConfigMapper.insertMcpServerConfig(BeanMappingUtils.map(config, McpServerConfigPO.class));
            return config;
        }
        mcpServerConfigMapper.updateMcpServerConfig(BeanMappingUtils.map(config, McpServerConfigPO.class));
        return config;
    }

    /**
     * 判断 MCP Server 配置是否存在
     * 用于删除与更新前置校验
     *
     * 避免更新/删除不存在的数据
     * 
     * @param query 主键查询条件。
     * @return `true` 表示配置存在，`false` 表示配置不存在。
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
     * 清理无效配置
     * 
     * @param query 主键查询条件。
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
     * 控制单次返回数量
     * 
     * @param query 分页查询条件。
     * @return MCP 服务配置列表。
     */
    @Override
    public List<McpServerConfig> findPage(McpServerConfigPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mcpServerConfigMapper.findPage(query), McpServerConfig.class);
    }

    /**
     * 查询启用的 MCP Server 配置
     * 用于运行时注册加载
     *
     * 运行时只加载启用配置
     * 
     * @param query 启用状态查询条件。
     * @return MCP 服务配置列表。
     */
    @Override
    public List<McpServerConfig> findByEnabled(EnabledQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mcpServerConfigMapper.findByEnabled(query), McpServerConfig.class);
    }

    /**
     * 统计 MCP Server 配置总数
     * 用于分页统计
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    @Override
    public long countAll() {
        return mcpServerConfigMapper.countAll();
    }
}
