package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolNameQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolRegistryPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * 工具注册仓储接口
 *
 * 职责：定义工具注册信息的持久化操作契约
 * @author sxie
 */
public interface McpToolRegistryRepository {

    /**
     * 按主键查询记录。
     */
    Optional<McpToolRegistry> findById(IdQuery query);

    /**
     * 按网关 ID 与工具名查询工具注册记录。
     */
    Optional<McpToolRegistry> findByGatewayIdAndToolName(ToolNameQuery query);

    /**
     * 按网关 ID 查询关联记录。
     */
    List<McpToolRegistry> findByGatewayId(GatewayIdQuery query);

    /**
     * 查询网关下已启用工具列表。
     */
    List<McpToolRegistry> findEnabledByGatewayId(GatewayIdQuery query);

    /**
     * 按条件分页查询记录。
     */
    List<McpToolRegistry> findPage(ToolRegistryPageQuery query);

    /**
     * 保存或更新记录。
     */
    McpToolRegistry save(McpToolRegistry registry);

    /**
     * 按主键删除记录。
     */
    void deleteById(IdQuery query);

    /**
     * 统计指定网关下的工具数量。
     */
    long countByGatewayId(GatewayIdQuery query);
}
