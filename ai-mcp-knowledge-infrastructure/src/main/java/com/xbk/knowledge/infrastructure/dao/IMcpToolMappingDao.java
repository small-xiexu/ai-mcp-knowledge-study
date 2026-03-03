package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpToolMappingPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolMappingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolIdQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工具参数映射 Mapper
 *
 * @author sxie
 */
@Mapper
public interface IMcpToolMappingDao extends BaseMapper<McpToolMappingPO> {

    /**
     * 新增参数映射记录
     * 
     * @param mapping 工具映射持久化实体。
     * @return 影响行数。
     */
     int insertToolMapping(McpToolMappingPO mapping);

    /**
     * 更新参数映射记录
     * 
     * @param mapping 工具映射持久化实体。
     * @return 影响行数。
     */
     int updateToolMapping(McpToolMappingPO mapping);

    /**
     * 按主键删除参数映射记录
     * 
     * @param query 主键查询条件。
     * @return 影响行数。
     */
     int deleteToolMappingById(IdQuery query);

    /**
     * 按工具 ID 批量删除参数映射（级联删除场景）
     * 
     * @param query 主键查询条件。
     * @return 影响行数。
     */
     int deleteToolMappingByToolId(ToolIdQuery query);

    /**
     * 按工具 ID + 映射类型（request/response）查询参数映射列表
     * 
     * @param query 工具映射查询条件。
     * @return McpToolMappingPO 列表。
     */
     List<McpToolMappingPO> findByToolIdAndMappingType(ToolMappingQuery query);

    /**
     * 按工具 ID 列表 + 映射类型（request/response）批量查询参数映射列表。
     *
     * @param toolIds 工具 ID 列表。
     * @param mappingType 映射类型。
     * @return McpToolMappingPO 列表。
     */
     List<McpToolMappingPO> findByToolIdsAndMappingType(@Param("toolIds") List<Long> toolIds,
                                                        @Param("mappingType") String mappingType);
}
