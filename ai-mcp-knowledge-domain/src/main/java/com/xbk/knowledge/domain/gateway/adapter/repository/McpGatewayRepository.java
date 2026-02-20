package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * 网关实例仓储接口
 *
 * 职责：定义网关实例的持久化操作契约
 * @author xiexu
 */
public interface McpGatewayRepository {

    /**
     * 根据业务 gatewayId 查询网关配置
     */
    Optional<McpGateway> findByGatewayId(GatewayIdQuery query);

    /**
     * 根据主键 ID 查询
     */
    Optional<McpGateway> findById(IdQuery query);

    /**
     * 保存或更新网关配置
     */
    McpGateway save(McpGateway gateway);

    /**
     * 根据主键删除
     */
    void deleteById(IdQuery query);

    /**
     * 分页查询网关列表
     */
    List<McpGateway> findPage(GatewayPageQuery query);

    /**
     * 查询所有启用的网关
     */
    List<McpGateway> findAllEnabled();

    /**
     * 统计总数
     */
    long countAll();
}
