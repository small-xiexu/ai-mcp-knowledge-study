package com.xbk.knowledge.domain.gateway.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具 ID 查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class ToolIdQuery {

    /**
     * 工具 ID。
     */
    private Long toolId;

    public ToolIdQuery(Long toolId) {
        this.toolId = toolId;
    }
}
