package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * 网关实例仓储接口
 *
 * 职责：定义网关实例的持久化操作契约
 * @author sxie
 */
public interface McpGatewayRepository {

    /**
     * 根据业务 gatewayId 查询网关配置
     * 
     * @param query 主键查询条件。
     * @return 可选的网关配置。
     */
    Optional<McpGateway> findByGatewayId(GatewayIdQuery query);

    /**
     * 根据主键 ID 查询
     * 
     * @param query 主键查询条件。
     * @return 可选的网关配置。
     */
    Optional<McpGateway> findById(IdQuery query);

    /**
     * 保存或更新网关配置
     * 
     * @param gateway 待保存的网关配置实体。
     * @return 已持久化的网关配置实体。
     */
    McpGateway save(McpGateway gateway);

    /**
     * 根据主键删除
     * 
     * @param query 主键查询条件。
     */
    void deleteById(IdQuery query);

    /**
     * 分页查询网关列表
     * 
     * @param query 分页查询条件。
     * @return 网关配置列表。
     */
    List<McpGateway> findPage(GatewayPageQuery query);

    /**
     * 查询所有启用的网关
     * 
     * @return 网关配置列表。
     */
    List<McpGateway> findAllEnabled();

    /**
     * 统计总数
     * 
     * @return 统计数量。
     */
    long countAll();
}
