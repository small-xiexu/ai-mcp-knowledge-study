package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolSchema;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工具 Schema 缓存 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface McpToolSchemaMapper extends BaseMapper<McpToolSchema> {

    int insertToolSchema(McpToolSchema schema);

    int updateToolSchema(McpToolSchema schema);

    McpToolSchema findActiveByGatewayIdAndToolId(@Param("gatewayId") String gatewayId,
                                                 @Param("toolId") Long toolId);
}
