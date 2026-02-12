package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayPageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Gateway 实例 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface McpGatewayMapper extends BaseMapper<McpGateway> {

    int insertGateway(McpGateway gateway);

    int updateGateway(McpGateway gateway);

    int deleteGatewayById(IdQuery query);

    McpGateway findById(IdQuery query);

    McpGateway findByGatewayId(GatewayIdQuery query);

    List<McpGateway> findPage(GatewayPageQuery query);

    List<McpGateway> findAllEnabled();

    long countAll();
}
