package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolNameQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolRegistryPageQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolRegistryRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IMcpToolRegistryDao;
import com.xbk.knowledge.infrastructure.dao.po.McpToolRegistryPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 工具注册仓储实现
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpToolRegistryRepositoryImpl implements McpToolRegistryRepository {

    private final IMcpToolRegistryDao mapper;

    /**
     * 查询MCP 工具注册。
     *
     * @param query 查询条件
     * @return 返回 McpToolRegistry 查询结果（可能为空）。
     */
    @Override
    public Optional<McpToolRegistry> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(query))
                .map(item -> BeanMappingUtils.map(item, McpToolRegistry.class));
    }

    /**
     * 查询MCP 工具注册。
     *
     * @param query 查询条件
     * @return 返回 McpToolRegistry 查询结果（可能为空）。
     */
    @Override
    public Optional<McpToolRegistry> findByGatewayIdAndToolName(ToolNameQuery query) {
        if (query == null || query.getGatewayId() == null || query.getToolName() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByGatewayIdAndToolName(query))
                .map(item -> BeanMappingUtils.map(item, McpToolRegistry.class));
    }

    /**
     * 查询MCP 工具注册。
     *
     * @param query 查询条件
     * @return 返回 McpToolRegistry 列表数据。
     */
    @Override
    public List<McpToolRegistry> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findByGatewayId(query), McpToolRegistry.class);
    }

    /**
     * 查询MCP 工具注册。
     *
     * @param query 查询条件
     * @return 返回 McpToolRegistry 列表数据。
     */
    @Override
    public List<McpToolRegistry> findEnabledByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findEnabledByGatewayId(query), McpToolRegistry.class);
    }

    /**
     * 查询MCP 工具注册。
     *
     * @param query 查询条件
     * @return 返回 McpToolRegistry 列表数据。
     */
    @Override
    public List<McpToolRegistry> findPage(ToolRegistryPageQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findPage(query), McpToolRegistry.class);
    }

    /**
     * 创建或更新MCP 工具注册数据。
     *
     * @param registry 工具注册配置。
     * @return 返回 McpToolRegistry 数据。
     */
    @Override
    public McpToolRegistry save(McpToolRegistry registry) {
        if (registry == null) {
            return null;
        }
        // 补齐 toolKey/riskLevel，保证治理字段稳定。
         * 约束：Gateway HTTP 工具默认使用 gateway:{gatewayId}:{toolName} 作为 toolKey。
        if (registry.getToolKey() == null || registry.getToolKey().isBlank()) {
            String gatewayId = registry.getGatewayId();
            String toolName = registry.getToolName();
            if (gatewayId != null && !gatewayId.isBlank() && toolName != null && !toolName.isBlank()) {
                registry.setToolKey("gateway:" + gatewayId + ":" + toolName);
            }
        }
        if (registry.getRiskLevel() == null || registry.getRiskLevel().isBlank()) {
            registry.setRiskLevel("MEDIUM");
        }
        if (registry.getId() == null) {
            mapper.insertToolRegistry(BeanMappingUtils.map(registry, McpToolRegistryPO.class));
            return registry;
        }
        mapper.updateToolRegistry(BeanMappingUtils.map(registry, McpToolRegistryPO.class));
        return registry;
    }

    /**
     * 删除MCP 工具注册数据。
     *
     * @param query 查询条件
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        mapper.deleteToolRegistryById(query);
    }

    /**
     * 按条件统计业务数量。
     *
     * @param query 查询条件
     * @return 统计数量
     */
    @Override
    public long countByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return 0;
        }
        return mapper.countByGatewayId(query);
    }
}
