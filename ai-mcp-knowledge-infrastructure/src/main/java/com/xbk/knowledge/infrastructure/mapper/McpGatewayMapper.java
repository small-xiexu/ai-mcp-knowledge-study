package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Gateway 实例 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface McpGatewayMapper extends BaseMapper<McpGateway> {

    /** 新增网关实例 */
    int insertGateway(McpGateway gateway);

    /** 更新网关实例 */
    int updateGateway(McpGateway gateway);

    /** 按主键删除网关实例 */
    int deleteGatewayById(IdQuery query);

    /** 按主键查询网关实例 */
    McpGateway findById(IdQuery query);

    /** 按 gatewayId（业务唯一标识）查询网关实例 */
    McpGateway findByGatewayId(GatewayIdQuery query);

    /** 分页查询网关列表 */
    List<McpGateway> findPage(GatewayPageQuery query);

    /** 查询所有已启用的网关实例 */
    List<McpGateway> findAllEnabled();

    /** 查询当前 org 下已启用的网关实例（强 org 隔离） */
    List<McpGateway> findAllEnabledByOrgId(@Param("orgId") Long orgId);

    /** 统计网关总数 */
    long countAll();

    /** 统计当前 org 下网关总数（强 org 隔离） */
    long countAllByOrgId(@Param("orgId") Long orgId);
}
