package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpGatewayPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayPageQuery;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * Gateway 实例 Mapper
 *
 * @author sxie
 */
@Mapper
public interface IMcpGatewayDao extends BaseMapper<McpGatewayPO> {

    /** 新增网关实例 */
    int insertGateway(McpGatewayPO gateway);

    /** 更新网关实例 */
    int updateGateway(McpGatewayPO gateway);

    /** 按主键删除网关实例 */
    int deleteGatewayById(IdQuery query);

    /** 按主键查询网关实例 */
    McpGatewayPO findById(IdQuery query);

    /** 按 gatewayId（业务唯一标识）查询网关实例 */
    McpGatewayPO findByGatewayId(GatewayIdQuery query);

    /** 分页查询网关列表 */
    List<McpGatewayPO> findPage(GatewayPageQuery query);

    /** 查询所有已启用的网关实例 */
    List<McpGatewayPO> findAllEnabled();

    /** 统计网关总数 */
    long countAll();
}
