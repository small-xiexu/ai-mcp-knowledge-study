package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolNameQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolRegistryPageQuery;
import com.xbk.knowledge.domain.repository.gateway.McpToolRegistryRepository;
import com.xbk.knowledge.infrastructure.mapper.McpToolRegistryMapper;
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

    @Override
    public Optional<McpToolRegistry> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(query));
    }

    @Override
    public Optional<McpToolRegistry> findByGatewayIdAndToolName(ToolNameQuery query) {
        if (query == null || query.getGatewayId() == null || query.getToolName() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByGatewayIdAndToolName(query));
    }

    @Override
    public List<McpToolRegistry> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return mapper.findByGatewayId(query);
    }

    @Override
    public List<McpToolRegistry> findEnabledByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return mapper.findEnabledByGatewayId(query);
    }

    @Override
    public List<McpToolRegistry> findPage(ToolRegistryPageQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return mapper.findPage(query);
    }

    @Override
    public McpToolRegistry save(McpToolRegistry registry) {
        if (registry == null) {
            return null;
        }
        if (registry.getId() == null) {
            mapper.insertToolRegistry(registry);
            return registry;
        }
        mapper.updateToolRegistry(registry);
        return registry;
    }

    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        mapper.deleteToolRegistryById(query);
    }

    @Override
    public long countByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return 0;
        }
        return mapper.countByGatewayId(query);
    }
}
