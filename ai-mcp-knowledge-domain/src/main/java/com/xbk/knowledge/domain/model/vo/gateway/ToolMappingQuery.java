package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具映射查询条件值对象
 *
 * 职责：承载基于工具ID和映射类型的查询条件
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class ToolMappingQuery {
    /** 组织ID */
    private Long orgId;
    /** 工具ID */
    private Long toolId;
    /** 映射类型：request/response */
    private String mappingType;

    public ToolMappingQuery(Long toolId, String mappingType) {
        this.toolId = toolId;
        this.mappingType = mappingType;
    }

    public ToolMappingQuery(Long orgId, Long toolId, String mappingType) {
        this.orgId = orgId;
        this.toolId = toolId;
        this.mappingType = mappingType;
    }
}
