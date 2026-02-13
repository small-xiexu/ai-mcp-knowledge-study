package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.gateway.McpGatewayAuth;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Gateway 鉴权 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface McpGatewayAuthMapper extends BaseMapper<McpGatewayAuth> {

    /** 新增鉴权记录 */
    int insertGatewayAuth(McpGatewayAuth auth);

    /** 更新鉴权记录 */
    int updateGatewayAuth(McpGatewayAuth auth);

    /** 按主键删除鉴权记录 */
    int deleteGatewayAuthById(Long id);

    /** 按 API Key 精确查询鉴权记录 */
    McpGatewayAuth findByApiKey(String apiKey);

    /** 按主键查询鉴权记录 */
    McpGatewayAuth findById(Long id);

    /** 按 gatewayId 查询该网关下所有鉴权记录 */
    List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query);
}
