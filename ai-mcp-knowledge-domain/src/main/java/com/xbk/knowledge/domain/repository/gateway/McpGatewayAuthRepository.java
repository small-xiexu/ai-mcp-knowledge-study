package com.xbk.knowledge.domain.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpGatewayAuth;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * 网关认证仓储接口
 *
 * 职责：定义网关认证信息的持久化操作契约
 * @author xiexu
 */
public interface McpGatewayAuthRepository {

    Optional<McpGatewayAuth> findById(Long id);

    Optional<McpGatewayAuth> findByApiKey(String apiKey);

    List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query);

    McpGatewayAuth save(McpGatewayAuth auth);

    void deleteById(Long id);
}
