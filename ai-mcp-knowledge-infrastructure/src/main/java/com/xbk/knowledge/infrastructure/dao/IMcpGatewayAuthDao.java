package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpGatewayAuthPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.gateway.model.entity.McpGatewayAuth;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Gateway 鉴权 Mapper
 *
 * @author sxie
 */
@Mapper
public interface IMcpGatewayAuthDao extends BaseMapper<McpGatewayAuthPO> {

    /** 新增鉴权记录 */
    int insertGatewayAuth(McpGatewayAuth auth);

    /** 更新鉴权记录 */
    int updateGatewayAuth(McpGatewayAuth auth);

    /** 按主键删除鉴权记录 */
    int deleteGatewayAuthById(Long id);

    /** 按主键 + scope 删除鉴权记录（强 scope 隔离） */
    int deleteGatewayAuthByIdAndScopeId(@Param("id") Long id);

    /** 按 API Key 精确查询鉴权记录 */
    McpGatewayAuth findByApiKey(String apiKey);

    /** 按 API Key + scope 精确查询鉴权记录（强 scope 隔离） */
    McpGatewayAuth findByApiKeyAndScopeId(@Param("apiKey") String apiKey);

    /** 按主键查询鉴权记录 */
    McpGatewayAuth findById(Long id);

    /** 按主键 + scope 查询鉴权记录（强 scope 隔离） */
    McpGatewayAuth findByIdAndScopeId(@Param("id") Long id);

    /** 按 gatewayId 查询该网关下所有鉴权记录 */
    List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query);
}
