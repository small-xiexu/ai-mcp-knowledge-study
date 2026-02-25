package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolSchema;

import java.util.Optional;

/**
 * 工具 Schema 缓存仓储接口
 *
 * 职责：定义工具 Schema 缓存的持久化操作契约
 * @author sxie
 */
public interface McpToolSchemaRepository {

    /**
     * 查询指定工具的活跃 Schema
     * 
     * @param gatewayId 标识 ID。
     * @param toolId 标识 ID。
     * @return 可选的活跃工具 Schema。
     */
    Optional<McpToolSchema> findActiveByGatewayIdAndToolId(String gatewayId, Long toolId);

    /**
     * 保存或更新 Schema 缓存
     * 
     * @param schema 待保存的工具 Schema 缓存实体。
     * @return 已持久化的工具 Schema 缓存实体。
     */
    McpToolSchema save(McpToolSchema schema);

    /**
     * 删除指定工具的 Schema 缓存
     * 
     * @param toolId 标识 ID。
     */
    void deleteByToolId(Long toolId);
}
