package com.xbk.knowledge.api.dto.mcp;

import com.xbk.knowledge.types.common.BaseRequest;
import com.xbk.knowledge.types.enums.McpServerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * MCP Server 配置请求 DTO
 * 用于创建和更新 MCP Server 配置
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class McpServerConfigRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 配置 ID（更新时必填，创建时不填）
     */
    private Long id;

    /**
     * MCP Server 名称
     */
    @NotBlank(message = "MCP Server 名称不能为空")
    private String serverName;

    /**
     * MCP Server 类型（STDIO/HTTP/SSE/WEBSOCKET）
     */
    @NotNull(message = "MCP Server 类型不能为空")
    private McpServerType serverType;

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;

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
}
