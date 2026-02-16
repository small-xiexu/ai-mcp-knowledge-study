package com.xbk.knowledge.infrastructure.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolSchema;
import com.xbk.knowledge.domain.repository.gateway.McpToolSchemaRepository;
import com.xbk.knowledge.infrastructure.mapper.gateway.McpToolSchemaMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 工具 Schema 缓存仓储实现
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class McpToolSchemaRepositoryImpl implements McpToolSchemaRepository {

    private final McpToolSchemaMapper mapper;

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

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
        return Optional.ofNullable(mapper.findActiveByGatewayIdAndToolId(currentOrgIdOrRoot(), gatewayId, toolId));
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
        if (schema.getOrgId() == null) {
            schema.setOrgId(currentOrgIdOrRoot());
        }
        if (schema.getId() == null) {
            mapper.insertToolSchema(schema);
            return schema;
        }
        mapper.updateToolSchema(schema);
        return schema;
    }
}
