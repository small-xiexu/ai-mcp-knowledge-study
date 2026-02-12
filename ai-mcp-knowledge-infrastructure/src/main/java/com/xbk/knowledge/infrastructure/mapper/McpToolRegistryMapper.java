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

    /** 新增工具注册记录 */
    int insertToolRegistry(McpToolRegistry registry);

    /** 更新工具注册记录 */
    int updateToolRegistry(McpToolRegistry registry);

    /** 按主键删除工具注册记录 */
    int deleteToolRegistryById(IdQuery query);

    /** 按主键查询工具注册记录 */
    McpToolRegistry findById(IdQuery query);

    /** 按 gatewayId + toolName 唯一定位工具 */
    McpToolRegistry findByGatewayIdAndToolName(ToolNameQuery query);

    /** 按 gatewayId 查询该网关下所有工具 */
    List<McpToolRegistry> findByGatewayId(GatewayIdQuery query);

    /** 按 gatewayId 查询该网关下已启用的工具 */
    List<McpToolRegistry> findEnabledByGatewayId(GatewayIdQuery query);

    /** 分页查询工具列表 */
    List<McpToolRegistry> findPage(ToolRegistryPageQuery query);

    /** 统计指定网关下的工具总数 */
    long countByGatewayId(GatewayIdQuery query);
}
