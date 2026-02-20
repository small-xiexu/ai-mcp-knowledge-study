package com.xbk.knowledge.domain.gateway.adapter.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;

import java.util.List;

/**
 * 工具绑定关系仓储接口
 *
 * 职责：定义工具绑定关系的持久化操作契约
 * @author xiexu
 */
public interface McpToolBindingRepository {

    /**
     * 查询指定绑定目标的所有工具绑定
     */
    List<McpToolBinding> findByBindTypeAndTargetId(ToolBindingQuery query);

    /**
     * 查询指定工具的所有绑定关系
     */
    List<McpToolBinding> findByToolId(Long toolId);

    /**
     * 保存绑定关系
     */
    McpToolBinding save(McpToolBinding binding);

    /**
     * 删除绑定关系
     */
    void deleteById(Long id);

    /**
     * 删除指定工具的所有绑定
     */
    void deleteByToolId(Long toolId);
}
