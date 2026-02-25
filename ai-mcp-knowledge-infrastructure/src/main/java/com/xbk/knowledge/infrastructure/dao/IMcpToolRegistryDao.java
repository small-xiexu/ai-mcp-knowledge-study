package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpToolRegistryPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolNameQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolRegistryPageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工具注册 Mapper
 *
 * @author sxie
 */
@Mapper
public interface IMcpToolRegistryDao extends BaseMapper<McpToolRegistryPO> {

    /**
     * 新增工具注册记录
     * 
     * @param registry 工具注册持久化实体。
     * @return 影响行数。
     */
     int insertToolRegistry(McpToolRegistryPO registry);

    /**
     * 更新工具注册记录
     * 
     * @param registry 工具注册持久化实体。
     * @return 影响行数。
     */
     int updateToolRegistry(McpToolRegistryPO registry);

    /**
     * 按主键删除工具注册记录
     * 
     * @param query 主键查询条件。
     * @return 影响行数。
     */
     int deleteToolRegistryById(IdQuery query);

    /**
     * 按主键查询工具注册记录
     * 
     * @param query 主键查询条件。
     * @return 工具注册持久化实体。
     */
     McpToolRegistryPO findById(IdQuery query);

    /**
     * 按 gatewayId + toolName 唯一定位工具
     * 
     * @param query 网关工具名称查询条件。
     * @return 工具注册持久化实体。
     */
     McpToolRegistryPO findByGatewayIdAndToolName(ToolNameQuery query);

    /**
     * 按 gatewayId 查询该网关下所有工具
     * 
     * @param query 主键查询条件。
     * @return McpToolRegistryPO 列表。
     */
     List<McpToolRegistryPO> findByGatewayId(GatewayIdQuery query);

    /**
     * 按 gatewayId 查询该网关下已启用的工具
     * 
     * @param query 主键查询条件。
     * @return McpToolRegistryPO 列表。
     */
     List<McpToolRegistryPO> findEnabledByGatewayId(GatewayIdQuery query);

    /**
     * 分页查询工具列表
     * 
     * @param query 分页查询条件。
     * @return McpToolRegistryPO 列表。
     */
     List<McpToolRegistryPO> findPage(ToolRegistryPageQuery query);

    /**
     * 统计指定网关下的工具总数
     * 
     * @param query 主键查询条件。
     * @return 统计数量。
     */
     long countByGatewayId(GatewayIdQuery query);
}
