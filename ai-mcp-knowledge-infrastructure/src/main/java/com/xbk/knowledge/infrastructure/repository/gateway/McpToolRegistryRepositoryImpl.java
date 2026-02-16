package com.xbk.knowledge.infrastructure.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolNameQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolRegistryPageQuery;
import com.xbk.knowledge.domain.repository.gateway.McpToolRegistryRepository;
import com.xbk.knowledge.infrastructure.mapper.gateway.McpToolRegistryMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
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

    private final McpToolRegistryMapper mapper;

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return mapper.countByGatewayId(query);
    }
}
