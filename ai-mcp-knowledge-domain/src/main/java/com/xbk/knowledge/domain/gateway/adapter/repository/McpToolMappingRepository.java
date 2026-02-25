package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolMappingQuery;

import java.util.List;

/**
 * 工具参数映射仓储接口
 *
 * 职责：定义工具参数映射的持久化操作契约
 * @author sxie
 */
public interface McpToolMappingRepository {

    /**
     * 按工具 ID 与映射类型查询映射配置。
     * 
     * @param query 工具映射查询条件。
     * @return 工具参数映射列表。
     */
    List<McpToolMapping> findByToolIdAndMappingType(ToolMappingQuery query);

    /**
     * 保存或更新记录。
     * 
     * @param mapping 待保存的工具参数映射实体。
     * @return 已持久化的工具参数映射实体。
     */
    McpToolMapping save(McpToolMapping mapping);

    /**
     * 按主键删除记录。
     * 
     * @param query 主键查询条件。
     */
    void deleteById(IdQuery query);

    /**
     * 删除指定工具的映射配置。
     * 
     * @param toolId 标识 ID。
     */
    void deleteByToolId(Long toolId);
}
