package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolMapping;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolMappingQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工具参数映射 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface McpToolMappingMapper extends BaseMapper<McpToolMapping> {

    int insertToolMapping(McpToolMapping mapping);

    int updateToolMapping(McpToolMapping mapping);

    int deleteToolMappingById(IdQuery query);

    int deleteToolMappingByToolId(Long toolId);

    List<McpToolMapping> findByToolIdAndMappingType(ToolMappingQuery query);
}
