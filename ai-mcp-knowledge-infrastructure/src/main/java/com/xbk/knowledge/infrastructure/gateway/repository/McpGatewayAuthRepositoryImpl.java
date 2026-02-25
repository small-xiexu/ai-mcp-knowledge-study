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
 * Gateway 鉴权仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpGatewayAuthRepositoryImpl implements McpGatewayAuthRepository {

    /**
     * Gateway 鉴权 DAO。
     */
    private final IMcpGatewayAuthDao mapper;

    /**
     * 查询MCP 网关鉴权。
     *
     * @param id 主键 ID
     * @return McpGatewayAuth 查询结果（可能为空）
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
     * 查询MCP 网关鉴权。
     *
     * @param apiKey API Key
     * @return McpGatewayAuth 查询结果（可能为空）
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
     * 查询MCP 网关鉴权。
     *
     * @param query 主键查询条件
     * @return McpGatewayAuth 列表
     */
    @Override
    public List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findByGatewayId(query), McpGatewayAuth.class);
    }

    /**
     * 创建或更新MCP 网关鉴权数据。
     *
     * @param auth 鉴权配置
     * @return 保存后的 McpGatewayAuth 信息
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
     * 删除MCP 网关鉴权数据。
     *
     * @param id 主键 ID
     */
    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        mapper.deleteGatewayAuthById(id);
    }

    /**
     * 删除MCP 网关鉴权数据。
     *
     * @param query 主键查询条件
     */
    @Override
    public void deleteByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return;
        }
        mapper.deleteByGatewayId(query);
    }
}
