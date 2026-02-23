package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpToolSchemaPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolIdQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工具 Schema 缓存 Mapper
 *
 * @author sxie
 */
@Mapper
public interface IMcpToolSchemaDao extends BaseMapper<McpToolSchemaPO> {

    /**
     * 新增 Schema 缓存记录
     */
     int insertToolSchema(McpToolSchemaPO schema);

    /**
     * 更新 Schema 缓存记录
     */
     int updateToolSchema(McpToolSchemaPO schema);

    /**
     * 按 gatewayId + toolId 查询当前生效的 Schema 快照
     */
     McpToolSchemaPO findActiveByGatewayIdAndToolId(@Param("gatewayId") String gatewayId,
                                                 @Param("toolId") Long toolId);

    /**
     * 按工具 ID 删除所有 Schema 快照
     */
     int deleteByToolId(ToolIdQuery query);
}
