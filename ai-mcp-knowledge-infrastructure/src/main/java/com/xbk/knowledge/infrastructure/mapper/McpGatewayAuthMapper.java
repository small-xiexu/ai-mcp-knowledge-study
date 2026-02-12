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

    int insertGatewayAuth(McpGatewayAuth auth);

    int updateGatewayAuth(McpGatewayAuth auth);

    int deleteGatewayAuthById(Long id);

    McpGatewayAuth findByApiKey(String apiKey);

    List<McpGatewayAuth> findByGatewayId(GatewayIdQuery query);

    List<McpGatewayAuth> findEnabledByGatewayId(GatewayIdQuery query);
}
