package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具ID查询条件值对象（按 org 隔离）。
 *
 * 职责：承载基于 toolId 的查询/删除条件，并补齐 orgId 以实现强隔离。
 
  * @author xiexu
  */
@Data
@NoArgsConstructor
public class ToolIdQuery {

    /** 组织ID */
    private Long orgId;

    /** 工具ID（mcp_tool_registry.id） */
    private Long toolId;

    /**
     * ToolIdQuery。
     *
     * @param toolId 参数
     */
    public ToolIdQuery(Long toolId) {
        this.toolId = toolId;
    }

    /**
     * ToolIdQuery。
     *
     * @param orgId 参数
     * @param toolId 参数
     */
    public ToolIdQuery(Long orgId, Long toolId) {
        this.orgId = orgId;
        this.toolId = toolId;
    }
}

