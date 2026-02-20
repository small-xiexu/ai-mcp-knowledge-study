package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpToolSchemaPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolSchema;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工具 Schema 缓存 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface IMcpToolSchemaDao extends BaseMapper<McpToolSchemaPO> {

    /** 新增 Schema 缓存记录 */
    int insertToolSchema(McpToolSchema schema);

    /** 更新 Schema 缓存记录 */
    int updateToolSchema(McpToolSchema schema);

    /** 按 gatewayId + toolId 查询当前生效的 Schema 快照 */
    McpToolSchema findActiveByGatewayIdAndToolId(@Param("gatewayId") String gatewayId,
                                                 @Param("toolId") Long toolId);
}
