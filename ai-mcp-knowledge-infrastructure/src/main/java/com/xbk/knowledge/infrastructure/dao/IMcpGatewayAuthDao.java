package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpGatewayAuthPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * Gateway 鉴权 Mapper
 *
 * @author sxie
 */
@Mapper
public interface IMcpGatewayAuthDao extends BaseMapper<McpGatewayAuthPO> {

    /**
     * 新增鉴权记录
     * 
     * @param auth 网关鉴权持久化实体。
     * @return 影响行数。
     */
     int insertGatewayAuth(McpGatewayAuthPO auth);

    /**
     * 更新鉴权记录
     * 
     * @param auth 网关鉴权持久化实体。
     * @return 影响行数。
     */
     int updateGatewayAuth(McpGatewayAuthPO auth);

    /**
     * 按主键删除鉴权记录
     * 
     * @param id 主键 ID。
     * @return 影响行数。
     */
     int deleteGatewayAuthById(Long id);

    /**
     * 按 API Key 精确查询鉴权记录
     * 
     * @param apiKey API Key。
     * @return 网关鉴权持久化实体。
     */
     McpGatewayAuthPO findByApiKey(String apiKey);

    /**
     * 按主键查询鉴权记录
     * 
     * @param id 主键 ID。
     * @return 网关鉴权持久化实体。
     */
     McpGatewayAuthPO findById(Long id);

    /**
     * 按 gatewayId 查询该网关下所有鉴权记录
     * 
     * @param query 主键查询条件。
     * @return 网关配置列表。
     */
     List<McpGatewayAuthPO> findByGatewayId(GatewayIdQuery query);

    /**
     * 按 gatewayId 删除该网关下所有鉴权记录
     * 
     * @param query 主键查询条件。
     * @return 影响行数。
     */
     int deleteByGatewayId(GatewayIdQuery query);
}
