package com.xbk.knowledge.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP 工具 Schema 实体
 * 对应数据库表：mcp_tool_schema
 *
 * 职责：领域实体，承载工具的 JSON Schema 版本快照与激活状态
 * @author sxie
 */
@TableName("mcp_tool_schema")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolSchemaPO {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 网关唯一标识
     *
     * 为什么：关联所属网关，支持多网关 Schema 隔离
     */
    private String gatewayId;

    /**
     * 工具ID
     *
     * 为什么：关联所属工具注册记录
     */
    private Long toolId;

    /**
     * Schema 版本号
     *
     * 为什么：支持 Schema 演进与版本回溯
     */
    private Integer schemaVersion;

    /**
     * 输入 Schema（JSON 格式）
     *
     * 为什么：定义工具入参的 JSON Schema，供 MCP 协议下发
     */
    private String inputSchema;

    /**
     * 输出 Schema（JSON 格式）
     *
     * 为什么：定义工具出参的 JSON Schema，用于响应校验
     */
    private String outputSchema;

    /**
     * Schema 哈希值
     *
     * 为什么：快速判断 Schema 是否变更，避免重复计算
     */
    private String schemaHash;

    /**
     * 是否激活
     *
     * 为什么：同一工具仅一个版本处于激活状态，支持灰度切换
     */
    private Boolean isActive;

    /**
     * 创建时间
     *
     * 为什么：用于审计与排序
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 为什么：用于审计与变更追踪
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
