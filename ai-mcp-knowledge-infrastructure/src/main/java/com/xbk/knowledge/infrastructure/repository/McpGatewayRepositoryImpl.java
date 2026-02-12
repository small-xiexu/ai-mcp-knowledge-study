package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayPageQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayRepository;
import com.xbk.knowledge.infrastructure.mapper.McpGatewayMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Gateway 实例仓储实现
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class McpGatewayRepositoryImpl implements McpGatewayRepository {

    private final McpGatewayMapper mapper;

    @Override
    public Optional<McpGateway> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByGatewayId(query));
    }

    @Override
    public Optional<McpGateway> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(query));
    }

    @Override
    public McpGateway save(McpGateway gateway) {
        if (gateway == null) {
            return null;
        }
        if (gateway.getId() == null) {
            mapper.insertGateway(gateway);
            return gateway;
        }
        mapper.updateGateway(gateway);
        return gateway;
    }

    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        mapper.deleteGatewayById(query);
    }

    @Override
    public List<McpGateway> findPage(GatewayPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return mapper.findPage(query);
    }

    @Override
    public List<McpGateway> findAllEnabled() {
        return mapper.findAllEnabled();
    }

    @Override
    public long countAll() {
        return mapper.countAll();
    }
}
