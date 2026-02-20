package com.xbk.knowledge.domain.gateway.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具名称查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class ToolNameQuery {

    private String gatewayId;
    private String toolName;

    public ToolNameQuery(String gatewayId, String toolName) {
        this.gatewayId = gatewayId;
        this.toolName = toolName;
    }
}
