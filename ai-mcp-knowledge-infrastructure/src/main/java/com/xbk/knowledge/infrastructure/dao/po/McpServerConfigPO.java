package com.xbk.knowledge.infrastructure.dao.po;

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
 * @author sxie
 */
@TableName("ai_mcp_server_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerConfigPO {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * MCP Server 名称
     *
     * 为什么：配置唯一标识，用于选择与展示
     */
    private String serverName;

    /**
     * MCP Server 类型（STDIO/HTTP/SSE/WEBSOCKET）
     *
     * 为什么：决定连接方式与协议
     */
    private McpServerType serverType;

    /**
     * 是否启用（0:禁用 1:启用）
     *
     * 为什么：控制是否参与运行时加载
     */
    private Boolean enabled;

    /**
     * 描述信息
     *
     * 为什么：便于运维识别与管理
     */
    private String description;

    /**
     * STDIO 模式命令
     *
     * 为什么：STDIO 模式需要启动命令
     */
    private String command;

    /**
     * STDIO 模式参数（JSON 数组）
     *
     * 为什么：参数可变，使用 JSON 保存
     */
    private String args;

    /**
     * STDIO 模式环境变量（JSON 对象）
     *
     * 为什么：环境变量可变，使用 JSON 保存
     */
    private String env;

    /**
     * 远程服务地址
     *
     * 为什么：HTTP/SSE/WS 模式需要远程地址
     */
    private String endpoint;

    /**
     * SSE 连接路径
     *
     * 为什么：SSE 模式需要独立连接路径
     */
    private String sseEndpoint;

    /**
     * HTTP Header（JSON 对象）
     *
     * 为什么：支持自定义鉴权与请求头
     */
    private String headers;

    /**
     * 连接超时（毫秒）
     *
     * 为什么：避免连接长期阻塞
     */
    private Integer connectTimeoutMs;

    /**
     * 请求超时（毫秒）
     *
     * 为什么：避免请求长期阻塞
     */
    private Integer requestTimeoutMs;

    /**
     * 初始化超时（毫秒）
     *
     * 为什么：控制初始化阶段等待时长
     */
    private Integer initTimeoutMs;

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
