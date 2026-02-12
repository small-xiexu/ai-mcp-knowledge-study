package com.xbk.knowledge.domain.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolMapping;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolMappingQuery;

import java.util.List;

/**
 * 工具参数映射仓储接口
 *
 * 职责：定义工具参数映射的持久化操作契约
 * @author xiexu
 */
public interface McpToolMappingRepository {

    List<McpToolMapping> findByToolIdAndMappingType(ToolMappingQuery query);

    McpToolMapping save(McpToolMapping mapping);

    void deleteById(IdQuery query);

    void deleteByToolId(Long toolId);
}
