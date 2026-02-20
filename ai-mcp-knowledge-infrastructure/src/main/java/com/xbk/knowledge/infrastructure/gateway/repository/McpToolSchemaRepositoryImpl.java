package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolSchema;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolSchemaRepository;
import com.xbk.knowledge.infrastructure.dao.IMcpToolSchemaDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 工具 Schema 缓存仓储实现
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpToolSchemaRepositoryImpl implements McpToolSchemaRepository {

    private final IMcpToolSchemaDao mapper;

    /**
     * findActiveByGatewayIdAndToolId。
     *
     * @param gatewayId 参数
     * @param toolId 参数
     * @return 返回结果
     */
    @Override
    public Optional<McpToolSchema> findActiveByGatewayIdAndToolId(String gatewayId, Long toolId) {
        if (gatewayId == null || toolId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findActiveByGatewayIdAndToolId(gatewayId, toolId));
    }

    /**
     * save。
     *
     * @param schema 参数
     * @return 返回结果
     */
    @Override
    public McpToolSchema save(McpToolSchema schema) {
        if (schema == null) {
            return null;
        }
        if (schema.getId() == null) {
            mapper.insertToolSchema(schema);
            return schema;
        }
        mapper.updateToolSchema(schema);
        return schema;
    }
}
