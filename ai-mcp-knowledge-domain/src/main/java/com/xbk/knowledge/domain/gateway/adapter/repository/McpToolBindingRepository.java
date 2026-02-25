package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;

import java.util.List;

/**
 * 工具绑定关系仓储接口
 *
 * 职责：定义工具绑定关系的持久化操作契约
 * @author sxie
 */
public interface McpToolBindingRepository {

    /**
     * 查询指定绑定目标的所有工具绑定
     * 
     * @param query 工具绑定查询条件。
     * @return 工具绑定关系列表。
     */
    List<McpToolBinding> findByBindTypeAndTargetId(ToolBindingQuery query);

    /**
     * 查询指定工具的所有绑定关系
     * 
     * @param toolId 标识 ID。
     * @return 工具绑定关系列表。
     */
    List<McpToolBinding> findByToolId(Long toolId);

    /**
     * 保存绑定关系
     * 
     * @param binding 待保存的工具绑定实体。
     * @return 已持久化的工具绑定实体。
     */
    McpToolBinding save(McpToolBinding binding);

    /**
     * 删除绑定关系
     * 
     * @param id 主键 ID。
     */
    void deleteById(Long id);

    /**
     * 删除指定工具的所有绑定
     * 
     * @param toolId 标识 ID。
     */
    void deleteByToolId(Long toolId);

    /**
     * 删除指定绑定目标下的所有绑定
     * 
     * @param query 工具绑定查询条件。
     */
    void deleteByBindTypeAndTargetId(ToolBindingQuery query);
}
