package com.xbk.knowledge.domain.model.vo.mcp;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server 配置分页查询条件值对象
 * 统一承载 MCP Server 配置分页条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class McpServerConfigPageQuery {

    /** 组织ID */
    private Long orgId;

    /**
     * 偏移量
     *
     * 为什么：用于分页计算起始位置
     */
    private Integer offset;

    /**
     * 每页大小
     *
     * 为什么：控制单次返回数量
     */
    private Integer pageSize;

    /**
     * McpServerConfigPageQuery。
     *
     * @param offset 参数
     * @param pageSize 参数
     */
    public McpServerConfigPageQuery(Integer offset, Integer pageSize) {
        this.offset = offset;
        this.pageSize = pageSize;
    }

    /**
     * McpServerConfigPageQuery。
     *
     * @param orgId 参数
     * @param offset 参数
     * @param pageSize 参数
     */
    public McpServerConfigPageQuery(Long orgId, Integer offset, Integer pageSize) {
        this.orgId = orgId;
        this.offset = offset;
        this.pageSize = pageSize;
    }
}
