package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.GatewayManageAppService;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayAuthRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolBindingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolMappingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolRegistryRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolSchemaRepository;
import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gateway 管理应用服务实现。
 *
 * 职责：统一网关资产删除时的级联清理逻辑，确保无外键场景下数据一致性。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class GatewayManageAppServiceImpl implements GatewayManageAppService {

    /**
     * 网关实例仓储。
     */
    private final McpGatewayRepository gatewayRepository;

    /**
     * 网关鉴权仓储。
     */
    private final McpGatewayAuthRepository gatewayAuthRepository;

    /**
     * 工具注册仓储。
     */
    private final McpToolRegistryRepository toolRegistryRepository;

    /**
     * 工具映射仓储。
     */
    private final McpToolMappingRepository toolMappingRepository;

    /**
     * 工具绑定仓储。
     */
    private final McpToolBindingRepository toolBindingRepository;

    /**
     * 工具 Schema 仓储。
     */
    private final McpToolSchemaRepository toolSchemaRepository;

    /**
     * 删除网关实例并执行应用层级联清理
     * 1. 删除网关下工具映射/绑定/schema
     * 2. 删除网关工具资产
     * 3. 删除网关凭证
     * 4. 删除网关实例
     * 
     * @param query 主键查询条件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGatewayInstance(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        McpGateway gateway = gatewayRepository.findById(query)
                .orElseThrow(() -> new NotFoundException("网关不存在"));
        GatewayIdQuery gatewayIdQuery = new GatewayIdQuery(gateway.getGatewayId());
        List<McpToolRegistry> tools = toolRegistryRepository.findByGatewayId(gatewayIdQuery);
        for (McpToolRegistry tool : tools) {
            if (tool == null || tool.getId() == null) {
                continue;
            }
            deleteToolCascade(tool.getId());
        }
        gatewayAuthRepository.deleteByGatewayId(gatewayIdQuery);
        gatewayRepository.deleteById(query);
    }

    /**
     * 删除工具并执行应用层级联清理
     * 1. 删除 request/response 参数映射
     * 2. 删除工具绑定
     * 3. 删除工具 schema
     * 4. 删除工具资产
     * 
     * @param query 主键查询条件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTool(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        Long toolId = query.getId();
        toolRegistryRepository.findById(query)
                .orElseThrow(() -> new NotFoundException("工具不存在"));
        deleteToolCascade(toolId);
    }

    private void deleteToolCascade(Long toolId) {
        toolMappingRepository.deleteByToolId(toolId);
        toolBindingRepository.deleteByToolId(toolId);
        toolSchemaRepository.deleteByToolId(toolId);
        toolRegistryRepository.deleteById(new IdQuery(toolId));
    }
}
