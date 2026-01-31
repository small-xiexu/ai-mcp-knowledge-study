package com.xbk.knowledge.domain.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xbk.knowledge.types.enums.McpServerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP Server 配置实体
 * 对应数据库表：ai_mcp_server_config
 *
 * 职责：领域实体，用于承载 MCP Server 配置状态
 * @author xiexu
 */
@TableName("ai_mcp_server_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * MCP Server 名称
     */
    private String serverName;

    /**
     * MCP Server 类型（STDIO/HTTP/SSE/WEBSOCKET）
     */
    private McpServerType serverType;

    /**
     * 是否启用（0:禁用 1:启用）
     */
    private Boolean enabled;

    /**
     * 描述信息
     */
    private String description;

    /**
     * STDIO 模式命令
     */
    private String command;

    /**
     * STDIO 模式参数（JSON 数组）
     */
    private String args;

    /**
     * STDIO 模式环境变量（JSON 对象）
     */
    private String env;

    /**
     * 远程服务地址
     */
    private String endpoint;

    /**
     * SSE 连接路径
     */
    private String sseEndpoint;

    /**
     * HTTP Header（JSON 对象）
     */
    private String headers;

    /**
     * 连接超时（毫秒）
     */
    private Integer connectTimeoutMs;

    /**
     * 请求超时（毫秒）
     */
    private Integer requestTimeoutMs;

    /**
     * 初始化超时（毫秒）
     */
    private Integer initTimeoutMs;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
