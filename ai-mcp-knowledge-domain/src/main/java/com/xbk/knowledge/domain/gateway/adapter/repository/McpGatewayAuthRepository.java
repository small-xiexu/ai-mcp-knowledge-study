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
     * 
     * @param id 主键 ID。
     * @return 可选的网关配置。
     */
    Optional<McpGatewayAuth> findById(Long id);

    /**
     * 按 API Key 查询鉴权配置。
     * 
     * @param apiKey API Key。
     * @return 可选的网关配置。
     */
    Optional<McpGatewayAuth> findByApiKey(String apiKey);

    /**
     * 按网关 ID 查询关联记录。
     * 
     * @param query 主键查询条件。
     * @return 网关配置列表。
     */
    List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query);

    /**
     * 保存或更新记录。
     * 
     * @param auth 待保存的网关认证实体。
     * @return 已持久化的网关认证实体。
     */
    McpGatewayAuth save(McpGatewayAuth auth);

    /**
     * 按主键删除记录。
     * 
     * @param id 主键 ID。
     */
    void deleteById(Long id);

    /**
     * 删除指定网关下的认证配置。
     * 
     * @param query 主键查询条件。
     */
    void deleteByGatewayId(GatewayIdQuery query);
}
