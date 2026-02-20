package com.xbk.knowledge.domain.gateway.model.entity;

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
 * MCP 网关实体
 * 对应数据库表：mcp_gateway
 *
 * 职责：领域实体，承载网关实例的核心配置与生命周期状态
 * @author xiexu
 */
@TableName("mcp_gateway")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGateway {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * scopeId。
     */

    /**
     * 网关唯一标识
     *
     * 为什么：业务层全局唯一键，解耦物理主键与业务引用
     */
    private String gatewayId;

    /**
     * 网关名称
     *
     * 为什么：面向用户的可读标识，用于展示与检索
     */
    private String gatewayName;

    /**
     * 网关描述
     *
     * 为什么：便于运维识别网关用途与管理
     */
    private String gatewayDesc;

    /**
     * 网关版本号
     *
     * 为什么：支持多版本并存与灰度发布
     */
    private String gatewayVersion;

    /**
     * 网关指令说明
     *
     * 为什么：为 LLM 提供网关级别的系统指令上下文
     */
    private String gatewayInstructions;

    /**
     * 状态（0:禁用 1:启用）
     *
     * 为什么：控制网关是否参与运行时路由
     */
    private Integer status;

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
