package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolNameQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolRegistryPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * 工具注册仓储接口
 *
 * 职责：定义工具注册信息的持久化操作契约
 * @author xiexu
 */
public interface McpToolRegistryRepository {

    /**
     * 方法：findById。
     */
    Optional<McpToolRegistry> findById(IdQuery query);

    /**
     * 方法：findByGatewayIdAndToolName。
     */
    Optional<McpToolRegistry> findByGatewayIdAndToolName(ToolNameQuery query);

    /**
     * 方法：findByGatewayId。
     */
    List<McpToolRegistry> findByGatewayId(GatewayIdQuery query);

    /**
     * 方法：findEnabledByGatewayId。
     */
    List<McpToolRegistry> findEnabledByGatewayId(GatewayIdQuery query);

    /**
     * 方法：findPage。
     */
    List<McpToolRegistry> findPage(ToolRegistryPageQuery query);

    /**
     * 方法：save。
     */
    McpToolRegistry save(McpToolRegistry registry);

    /**
     * 方法：deleteById。
     */
    void deleteById(IdQuery query);

    /**
     * 方法：countByGatewayId。
     */
    long countByGatewayId(GatewayIdQuery query);
}
