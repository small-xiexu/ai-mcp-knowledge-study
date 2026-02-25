package com.xbk.knowledge.domain.gateway.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP 工具 Schema 实体
 * 对应数据库表mcp_tool_schema
 *
 * 职责：领域实体，承载工具的 JSON Schema 版本快照与激活状态
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolSchema {

    /**
     * 主键ID
     *
     * 用于持久化唯一标识
     */
    private Long id;

    /**
     * 网关唯一标识
     *
     * 关联所属网关，支持多网关 Schema 隔离
     */
    private String gatewayId;

    /**
     * 工具ID
     *
     * 关联所属工具注册记录
     */
    private Long toolId;

    /**
     * Schema 版本号
     *
     * 支持 Schema 演进与版本回溯
     */
    private Integer schemaVersion;

    /**
     * 输入 Schema（JSON 格式）
     *
     * 定义工具入参的 JSON Schema，供 MCP 协议下发
     */
    private String inputSchema;

    /**
     * 输出 Schema（JSON 格式）
     *
     * 定义工具出参的 JSON Schema，用于响应校验
     */
    private String outputSchema;

    /**
     * Schema 哈希值
     *
     * 快速判断 Schema 是否变更，避免重复计算
     */
    private String schemaHash;

    /**
     * 是否激活
     *
     * 同一工具仅一个版本处于激活状态，支持灰度切换
     */
    private Boolean isActive;

    /**
     * 创建时间
     *
     * 用于审计与排序
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 用于审计与变更追踪
     */
    private LocalDateTime updatedAt;
}
