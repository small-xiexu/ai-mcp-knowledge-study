package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpGatewayAuth;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * 网关认证仓储接口
 *
 * 职责：定义网关认证信息的持久化操作契约
 * @author sxie
 */
public interface McpGatewayAuthRepository {

    /**
     * 方法：findById。
     */
    Optional<McpGatewayAuth> findById(Long id);

    /**
     * 方法：findByApiKey。
     */
    Optional<McpGatewayAuth> findByApiKey(String apiKey);

    /**
     * 方法：findByGatewayId。
     */
    List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query);

    /**
     * 方法：save。
     */
    McpGatewayAuth save(McpGatewayAuth auth);

    /**
     * 方法：deleteById。
     */
    void deleteById(Long id);

    /**
     * 方法：deleteByGatewayId。
     */
    void deleteByGatewayId(GatewayIdQuery query);
}
