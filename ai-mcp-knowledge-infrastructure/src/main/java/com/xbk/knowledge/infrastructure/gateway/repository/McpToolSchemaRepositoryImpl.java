package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolSchema;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolIdQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolSchemaRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IMcpToolSchemaDao;
import com.xbk.knowledge.infrastructure.dao.po.McpToolSchemaPO;
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

    /**
     * 工具 Schema DAO。
     */
    private final IMcpToolSchemaDao mapper;

    /**
     * 查询MCP 工具 Schema。
     * 
     * @param gatewayId 网关 ID
     * @param toolId 工具 ID
     * @return McpToolSchema 查询结果（可能为空）。
     */
    @Override
    public Optional<McpToolSchema> findActiveByGatewayIdAndToolId(String gatewayId, Long toolId) {
        if (gatewayId == null || toolId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findActiveByGatewayIdAndToolId(gatewayId, toolId))
                .map(item -> BeanMappingUtils.map(item, McpToolSchema.class));
    }

    /**
     * 创建或更新MCP 工具 Schema数据。
     * 
     * @param schema 工具 Schema 配置。
     * @return McpToolSchema 数据。
     */
    @Override
    public McpToolSchema save(McpToolSchema schema) {
        if (schema == null) {
            return null;
        }
        if (schema.getId() == null) {
            mapper.insertToolSchema(BeanMappingUtils.map(schema, McpToolSchemaPO.class));
            return schema;
        }
        mapper.updateToolSchema(BeanMappingUtils.map(schema, McpToolSchemaPO.class));
        return schema;
    }

    /**
     * 删除MCP 工具 Schema数据。
     * 
     * @param toolId 工具 ID
     */
    @Override
    public void deleteByToolId(Long toolId) {
        if (toolId == null) {
            return;
        }
        mapper.deleteByToolId(new ToolIdQuery(toolId));
    }
}
