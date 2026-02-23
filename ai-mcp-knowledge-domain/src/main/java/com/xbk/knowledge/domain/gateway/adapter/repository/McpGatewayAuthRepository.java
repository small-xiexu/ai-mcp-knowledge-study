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
     * 按主键查询记录。
     */
    Optional<McpGatewayAuth> findById(Long id);

    /**
     * 按 API Key 查询鉴权配置。
     */
    Optional<McpGatewayAuth> findByApiKey(String apiKey);

    /**
     * 按网关 ID 查询关联记录。
     */
    List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query);

    /**
     * 保存或更新记录。
     */
    McpGatewayAuth save(McpGatewayAuth auth);

    /**
     * 按主键删除记录。
     */
    void deleteById(Long id);

    /**
     * 删除指定网关下的认证配置。
     */
    void deleteByGatewayId(GatewayIdQuery query);
}
