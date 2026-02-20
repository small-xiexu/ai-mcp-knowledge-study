package com.xbk.knowledge.api.dto.mcp;

import com.xbk.knowledge.types.enums.McpServerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MCP Server 配置响应 DTO
 * 用于返回 MCP Server 配置信息
 *
 * 职责：接口层 DTO，用于承载响应数据并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerConfigResponse {

    /**
     * 配置 ID
     */
    private Long id;

    /**
     * MCP Server 名称
     */
    private String serverName;

    /**
     * MCP Server 类型
     */
    private McpServerType serverType;

    /**
     * 是否启用
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
     * STDIO 模式参数
     */
    private List<String> args;

    /**
     * STDIO 模式环境变量
     */
    private Map<String, String> env;

    /**
     * 远程服务地址
     */
    private String endpoint;

    /**
     * SSE 连接路径
     */
    private String sseEndpoint;

    /**
     * HTTP Header
     */
    private Map<String, String> headers;

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
     * 运行状态
     */
    private Boolean running;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
