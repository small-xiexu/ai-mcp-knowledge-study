package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpGatewayAuth;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayAuthRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IMcpGatewayAuthDao;
import com.xbk.knowledge.infrastructure.dao.po.McpGatewayAuthPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Gateway 鉴权仓储实现
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpGatewayAuthRepositoryImpl implements McpGatewayAuthRepository {

    private final IMcpGatewayAuthDao mapper;

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
        return Optional.ofNullable(mapper.findById(id))
                .map(item -> BeanMappingUtils.map(item, McpGatewayAuth.class));
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
        return Optional.ofNullable(mapper.findByApiKey(apiKey))
                .map(item -> BeanMappingUtils.map(item, McpGatewayAuth.class));
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
        return BeanMappingUtils.mapList(mapper.findByGatewayId(query), McpGatewayAuth.class);
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
            mapper.insertGatewayAuth(BeanMappingUtils.map(auth, McpGatewayAuthPO.class));
            return auth;
        }
        mapper.updateGatewayAuth(BeanMappingUtils.map(auth, McpGatewayAuthPO.class));
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
        mapper.deleteGatewayAuthById(id);
    }
}
