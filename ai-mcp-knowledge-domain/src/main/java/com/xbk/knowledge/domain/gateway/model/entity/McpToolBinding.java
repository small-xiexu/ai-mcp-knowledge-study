package com.xbk.knowledge.domain.gateway.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP 工具绑定实体
 * 对应数据库表：mcp_tool_binding
 *
 * 职责：领域实体，承载工具与目标资源之间的绑定关系与启停控制
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolBinding {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    private Long id;

    /**
     * 网关唯一标识
     *
     * 为什么：关联所属网关，支持多网关绑定隔离
     */
    private String gatewayId;

    /**
     * 工具ID
     *
     * 为什么：关联所属工具注册记录
     */
    private Long toolId;

    /**
     * 绑定类型（USER/ROLE/GROUP）
     *
     * 为什么：区分不同维度的绑定策略，支持灵活的权限分配
     */
    private String bindType;

    /**
     * 绑定目标ID
     *
     * 为什么：指向具体的用户/角色/分组，与 bindType 配合定位目标
     */
    private Long bindTargetId;

    /**
     * 是否启用
     *
     * 为什么：支持临时禁用绑定而不删除记录
     */
    private Boolean enabled;

    /**
     * 创建时间
     *
     * 为什么：用于审计与排序
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 为什么：用于审计与变更追踪
     */
    private LocalDateTime updatedAt;
}
