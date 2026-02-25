package com.xbk.knowledge.domain.gateway.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP 网关鉴权实体
 * 对应数据库表mcp_gateway_auth
 *
 * 职责：领域实体，承载网关的 API 鉴权凭证与限流策略
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayAuth {

    /**
     * 主键ID
     *
     * 用于持久化唯一标识
     */
    private Long id;

    /**
     * 网关唯一标识
     *
     * 关联所属网关，支持多网关独立鉴权
     */
    private String gatewayId;

    /**
     * API 密钥
     *
     * 调用方身份凭证，用于请求鉴权
     */
    private String apiKey;

    /**
     * 速率限制（次/分钟）
     *
     * 防止单个调用方过载，保护后端服务稳定性
     */
    private Integer rateLimit;

    /**
     * 密钥过期时间
     *
     * 限定凭证有效期，降低泄露风险
     */
    private LocalDateTime expireTime;

    /**
     * 状态（0:禁用 1:启用）
     *
     * 支持紧急吊销凭证而不删除记录
     */
    private Integer status;

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
