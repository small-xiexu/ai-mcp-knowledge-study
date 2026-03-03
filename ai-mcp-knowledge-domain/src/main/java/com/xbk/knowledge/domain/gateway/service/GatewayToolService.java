package com.xbk.knowledge.domain.gateway.service;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 网关工具域服务接口
 *
 * 职责：Gateway 域的核心业务能力，提供工具清单查询、工具调用执行、Schema 生成等能力。
 * 内部路径（GatewayToolCallbackProvider）和外部路径（SSE Controller）共享此服务实例，
 * 确保两条路径的业务行为完全一致。
 *
 * @author sxie
 */
public interface GatewayToolService {

    /**
     * 查询指定网关下所有已启用工具的清单（含 inputSchema）
     *
     * 等价于 MCP 协议的 tools/list，供内部 ToolCallbackProvider 和外部 SSE 路径共用。
     * 内部自动处理 Schema 缓存（基于 SHA-256 hash 判断是否需要重新生成）。
     * 
     * @param gatewayId 网关唯一标识
     * @return 工具清单列表，每个元素包含 name、description、inputSchema
     */
    List<ToolDefinition> listTools(String gatewayId);

    /**
     * 执行工具调用（调用外部 HTTP 接口）
     *
     * 等价于 MCP 协议的 tools/call，核心链路为
     * 参数映射 → 鉴权注入 → HTTP 请求 → 响应提取。
     * 支持超时控制和重试机制。
     * 
     * @param gatewayId 网关唯一标识
     * @param toolName 工具名称
     * @param arguments 调用参数（模型传入的 JSON 参数）
     * @return 工具调用结果（结构化文本）
     */
    ToolCallResult callTool(String gatewayId, String toolName, Map<String, Object> arguments);

    /**
     * 获取网关能力声明（供外部 SSE 路径的 initialize 握手使用）
     *
     * 等价于 MCP 协议的 initialize 响应，返回网关名称、版本、支持的能力等信息。
     * 
     * @param gatewayId 网关唯一标识
     * @return 网关能力声明
     */
    GatewayInfo initialize(String gatewayId);

    // ========== 内部数据结构 ==========

    /**
     * 工具定义（tools/list 的单个工具）。
     */
    @EqualsAndHashCode
    @ToString
    final class ToolDefinition {
        /**
         * 工具名称。
         */
        private final String name;

        /**
         * 工具描述。
         */
        private final String description;

        /**
         * 输入参数的 JSON Schema。
         */
        private final Map<String, Object> inputSchema;

        public ToolDefinition(String name, String description, Map<String, Object> inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }

        public String getName() {
            return name;
        }

        public String name() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String description() {
            return description;
        }

        public Map<String, Object> getInputSchema() {
            return inputSchema;
        }

        public Map<String, Object> inputSchema() {
            return inputSchema;
        }
    }

    /**
     * 工具调用结果。
     */
    @lombok.EqualsAndHashCode
    @lombok.ToString
    final class ToolCallResult {
        /**
         * 是否调用成功。
         */
        private final boolean success;

        /**
         * 结果内容（成功时为响应数据，失败时为错误信息）。
         */
        private final String content;

        /**
         * 错误码（失败时有值）。
         */
        private final String errorCode;

        public ToolCallResult(boolean success, String content, String errorCode) {
            this.success = success;
            this.content = content;
            this.errorCode = errorCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean success() {
            return success;
        }

        public String getContent() {
            return content;
        }

        public String content() {
            return content;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String errorCode() {
            return errorCode;
        }
    }

    /**
     * 网关能力声明（initialize 响应）。
     */
    @lombok.EqualsAndHashCode
    @lombok.ToString
    final class GatewayInfo {
        /**
         * 协议版本。
         */
        private final String protocolVersion;

        /**
         * 网关名称。
         */
        private final String serverName;

        /**
         * 网关版本。
         */
        private final String serverVersion;

        /**
         * 网关使用说明。
         */
        private final String instructions;

        public GatewayInfo(String protocolVersion, String serverName, String serverVersion, String instructions) {
            this.protocolVersion = protocolVersion;
            this.serverName = serverName;
            this.serverVersion = serverVersion;
            this.instructions = instructions;
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public String protocolVersion() {
            return protocolVersion;
        }

        public String getServerName() {
            return serverName;
        }

        public String serverName() {
            return serverName;
        }

        public String getServerVersion() {
            return serverVersion;
        }

        public String serverVersion() {
            return serverVersion;
        }

        public String getInstructions() {
            return instructions;
        }

        public String instructions() {
            return instructions;
        }
    }
}
