package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.gateway.McpGatewayAuth;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayAuthRepository;
import com.xbk.knowledge.infrastructure.mapper.McpGatewayAuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Gateway 鉴权仓储实现
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class McpGatewayAuthRepositoryImpl implements McpGatewayAuthRepository {

    private final McpGatewayAuthMapper mapper;

    @Override
    public Optional<McpGatewayAuth> findByApiKey(String apiKey) {
        if (apiKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByApiKey(apiKey));
    }

    @Override
    public List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return mapper.findByGatewayId(query);
    }

    @Override
    public McpGatewayAuth save(McpGatewayAuth auth) {
        if (auth == null) {
            return null;
        }
        if (auth.getId() == null) {
            mapper.insertGatewayAuth(auth);
            return auth;
        }
        mapper.updateGatewayAuth(auth);
        return auth;
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        mapper.deleteGatewayAuthById(id);
    }
}
