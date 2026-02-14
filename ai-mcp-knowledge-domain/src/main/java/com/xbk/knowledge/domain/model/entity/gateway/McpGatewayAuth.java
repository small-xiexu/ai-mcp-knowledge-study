package com.xbk.knowledge.domain.model.entity.gateway;

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
 * MCP 网关鉴权实体
 * 对应数据库表：mcp_gateway_auth
 *
 * 职责：领域实体，承载网关的 API 鉴权凭证与限流策略
 * @author xiexu
 */
@TableName("mcp_gateway_auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayAuth {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 组织ID。
     */
    private Long orgId;

    /**
     * 网关唯一标识
     *
     * 为什么：关联所属网关，支持多网关独立鉴权
     */
    private String gatewayId;

    /**
     * API 密钥
     *
     * 为什么：调用方身份凭证，用于请求鉴权
     */
    private String apiKey;

    /**
     * 速率限制（次/分钟）
     *
     * 为什么：防止单个调用方过载，保护后端服务稳定性
     */
    private Integer rateLimit;

    /**
     * 密钥过期时间
     *
     * 为什么：限定凭证有效期，降低泄露风险
     */
    private LocalDateTime expireTime;

    /**
     * 状态（0:禁用 1:启用）
     *
     * 为什么：支持紧急吊销凭证而不删除记录
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
