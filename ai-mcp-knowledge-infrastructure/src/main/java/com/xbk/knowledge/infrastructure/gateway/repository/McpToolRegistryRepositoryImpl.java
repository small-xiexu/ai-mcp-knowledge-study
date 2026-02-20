package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolNameQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolRegistryPageQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolRegistryRepository;
import com.xbk.knowledge.infrastructure.dao.IMcpToolRegistryDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 工具注册仓储实现
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class McpToolRegistryRepositoryImpl implements McpToolRegistryRepository {

    private final IMcpToolRegistryDao mapper;

    /**
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<McpToolRegistry> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(query));
    }

    /**
     * findByGatewayIdAndToolName。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<McpToolRegistry> findByGatewayIdAndToolName(ToolNameQuery query) {
        if (query == null || query.getGatewayId() == null || query.getToolName() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByGatewayIdAndToolName(query));
    }

    /**
     * findByGatewayId。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<McpToolRegistry> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return mapper.findByGatewayId(query);
    }

    /**
     * findEnabledByGatewayId。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<McpToolRegistry> findEnabledByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return mapper.findEnabledByGatewayId(query);
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<McpToolRegistry> findPage(ToolRegistryPageQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return mapper.findPage(query);
    }

    /**
     * save。
     *
     * @param registry 参数
     * @return 返回结果
     */
    @Override
    public McpToolRegistry save(McpToolRegistry registry) {
        if (registry == null) {
            return null;
        }
        /*
         * 目的：补齐 toolKey/riskLevel，保证治理字段稳定。
         * 约束：Gateway HTTP 工具默认使用 gateway:{gatewayId}:{toolName} 作为 toolKey。
         */
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
            mapper.insertToolRegistry(registry);
            return registry;
        }
        mapper.updateToolRegistry(registry);
        return registry;
    }

    /**
     * deleteById。
     *
     * @param query 参数
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        mapper.deleteToolRegistryById(query);
    }

    /**
     * countByGatewayId。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long countByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return 0;
        }
        return mapper.countByGatewayId(query);
    }
}
