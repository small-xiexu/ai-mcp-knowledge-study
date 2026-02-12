package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class ToolMappingQuery {
    /** 工具ID */
    private Long toolId;
    /** 映射类型：request/response */
    private String mappingType;
}
