package com.xbk.knowledge.domain.model.vo.mcp;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server 名称查询条件值对象
 * 统一承载名称唯一性校验条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class McpServerNameQuery {

    /** 组织ID */
    private Long orgId;

    /**
     * MCP Server 名称
     *
     * 为什么：用于唯一性校验与定位配置
     */
    private String serverName;

    public McpServerNameQuery(String serverName) {
        this.serverName = serverName;
    }

    public McpServerNameQuery(Long orgId, String serverName) {
        this.orgId = orgId;
        this.serverName = serverName;
    }
}
