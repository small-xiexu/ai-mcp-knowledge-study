package com.xbk.knowledge.infrastructure.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpGatewayAuth;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayAuthRepository;
import com.xbk.knowledge.infrastructure.mapper.gateway.McpGatewayAuthMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
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

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

    /**
     * findById。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Optional<McpGatewayAuth> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByIdAndOrgId(currentOrgIdOrRoot(), id));
    }

    /**
     * findByApiKey。
     *
     * @param apiKey 参数
     * @return 返回结果
     */
    @Override
    public Optional<McpGatewayAuth> findByApiKey(String apiKey) {
        if (apiKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByApiKeyAndOrgId(currentOrgIdOrRoot(), apiKey));
    }

    /**
     * findByGatewayId。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return mapper.findByGatewayId(query);
    }

    /**
     * save。
     *
     * @param auth 参数
     * @return 返回结果
     */
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

    /**
     * deleteById。
     *
     * @param id 参数
     */
    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        mapper.deleteGatewayAuthByIdAndOrgId(currentOrgIdOrRoot(), id);
    }
}
