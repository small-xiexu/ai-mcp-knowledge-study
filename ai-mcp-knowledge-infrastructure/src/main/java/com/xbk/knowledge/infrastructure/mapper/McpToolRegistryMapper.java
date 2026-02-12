package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolNameQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolRegistryPageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工具注册 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface McpToolRegistryMapper extends BaseMapper<McpToolRegistry> {

    int insertToolRegistry(McpToolRegistry registry);

    int updateToolRegistry(McpToolRegistry registry);

    int deleteToolRegistryById(IdQuery query);

    McpToolRegistry findById(IdQuery query);

    McpToolRegistry findByGatewayIdAndToolName(ToolNameQuery query);

    List<McpToolRegistry> findByGatewayId(GatewayIdQuery query);

    List<McpToolRegistry> findEnabledByGatewayId(GatewayIdQuery query);

    List<McpToolRegistry> findPage(ToolRegistryPageQuery query);

    long countByGatewayId(GatewayIdQuery query);
}
