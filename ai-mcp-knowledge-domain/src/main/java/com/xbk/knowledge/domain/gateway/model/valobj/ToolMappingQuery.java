package com.xbk.knowledge.domain.gateway.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具参数映射查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class ToolMappingQuery {

    /**
     * 工具 ID。
     */
    private Long toolId;

    /**
     * 映射类型。
     */
    private String mappingType;

    public ToolMappingQuery(Long toolId, String mappingType) {
        this.toolId = toolId;
        this.mappingType = mappingType;
    }
}
