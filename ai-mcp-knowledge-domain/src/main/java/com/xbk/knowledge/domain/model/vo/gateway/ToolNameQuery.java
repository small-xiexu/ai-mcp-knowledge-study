package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具名称查询条件值对象
 *
 * 职责：承载基于网关ID和工具名称的查询条件
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class ToolNameQuery {
    /** 组织ID */
    private Long orgId;
    /** 网关唯一标识 */
    private String gatewayId;
    /** 工具名称 */
    private String toolName;

    public ToolNameQuery(String gatewayId, String toolName) {
        this.gatewayId = gatewayId;
        this.toolName = toolName;
    }

    public ToolNameQuery(Long orgId, String gatewayId, String toolName) {
        this.orgId = orgId;
        this.gatewayId = gatewayId;
        this.toolName = toolName;
    }
}
