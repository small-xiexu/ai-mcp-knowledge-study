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
 * MCP 工具注册实体
 * 对应数据库表mcp_tool_registry
 *
 * 职责：领域实体，承载网关中注册的工具元数据与 HTTP 调用配置
 * @author sxie
 */
@TableName("mcp_tool_registry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolRegistryPO {

    /**
     * 主键ID
     *
     * 用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 网关唯一标识
     *
     * 关联所属网关，支持多网关工具隔离
     */
    private String gatewayId;

    /**
     * 工具名称
     *
     * MCP 协议中工具的唯一标识符
     */
    private String toolName;

    /**
     * 工具唯一键（平台统一标识）。
     *
     * 约定
     * - Gateway HTTP 工具gateway:{gatewayId}:{toolName}
     * - MCP 工具mcp:{serverName}:{toolName}
     */
    private String toolKey;

    /**
     * 工具描述
     *
     * 供 LLM 理解工具用途，影响工具选择决策
     */
    private String toolDescription;

    /**
     * HTTP 请求地址
     *
     * 工具实际调用的后端服务 URL
     */
    private String httpUrl;

    /**
     * HTTP 请求方法。
     */
    private String httpMethod;

    /**
     * HTTP 请求头（JSON 格式）
     *
     * 支持自定义鉴权与内容协商等请求头
     */
    private String httpHeaders;

    /**
     * 超时时间（毫秒）
     *
     * 避免工具调用长期阻塞网关线程
     */
    private Integer timeout;

    /**
     * 重试次数
     *
     * 提升瞬时故障下的调用成功率
     */
    private Integer retryTimes;

    /**
     * 风险等级LOW/MEDIUM/HIGH。
     *
     * 治理门禁与审批策略需要风险分级；HIGH 默认生成审批单。
     */
    private String riskLevel;

    /**
     * 状态（0:禁用 1:启用）
     *
     * 控制工具是否对外暴露
     */
    private Integer status;

    /**
     * 创建时间
     *
     * 用于审计与排序
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 用于审计与变更追踪
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
