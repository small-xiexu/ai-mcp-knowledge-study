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
     * 方法：findByToolIdAndMappingType。
     */
    List<McpToolMapping> findByToolIdAndMappingType(ToolMappingQuery query);

    /**
     * 方法：save。
     */
    McpToolMapping save(McpToolMapping mapping);

    /**
     * 方法：deleteById。
     */
    void deleteById(IdQuery query);

    /**
     * 方法：deleteByToolId。
     */
    void deleteByToolId(Long toolId);
}
