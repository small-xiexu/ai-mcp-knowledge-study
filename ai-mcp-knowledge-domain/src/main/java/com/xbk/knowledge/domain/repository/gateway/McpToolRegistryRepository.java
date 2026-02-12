package com.xbk.knowledge.domain.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolNameQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolRegistryPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * 工具注册仓储接口
 *
 * 职责：定义工具注册信息的持久化操作契约
 * @author xiexu
 */
public interface McpToolRegistryRepository {

    Optional<McpToolRegistry> findById(IdQuery query);

    Optional<McpToolRegistry> findByGatewayIdAndToolName(ToolNameQuery query);

    List<McpToolRegistry> findByGatewayId(GatewayIdQuery query);

    List<McpToolRegistry> findEnabledByGatewayId(GatewayIdQuery query);

    List<McpToolRegistry> findPage(ToolRegistryPageQuery query);

    McpToolRegistry save(McpToolRegistry registry);

    void deleteById(IdQuery query);

    long countByGatewayId(GatewayIdQuery query);
}
