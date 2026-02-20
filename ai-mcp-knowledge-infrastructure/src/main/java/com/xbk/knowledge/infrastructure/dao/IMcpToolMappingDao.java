package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpToolMappingPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolMappingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolIdQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工具参数映射 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface IMcpToolMappingDao extends BaseMapper<McpToolMappingPO> {

    /** 新增参数映射记录 */
    int insertToolMapping(McpToolMapping mapping);

    /** 更新参数映射记录 */
    int updateToolMapping(McpToolMapping mapping);

    /** 按主键删除参数映射记录 */
    int deleteToolMappingById(IdQuery query);

    /** 按工具 ID 批量删除参数映射（级联删除场景） */
    int deleteToolMappingByToolId(ToolIdQuery query);

    /** 按工具 ID + 映射类型（request/response）查询参数映射列表 */
    List<McpToolMapping> findByToolIdAndMappingType(ToolMappingQuery query);
}
