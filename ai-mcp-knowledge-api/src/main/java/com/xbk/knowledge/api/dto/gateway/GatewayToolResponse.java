package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Gateway 工具响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayToolResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 工具主键 ID。
     */
    private Long id;

    /**
     * 所属网关 ID。
     */
    private String gatewayId;

    /**
     * 工具名称。
     */
    private String toolName;

    /**
     * 工具唯一键。
     */
    private String toolKey;

    /**
     * 工具描述。
     */
    private String toolDescription;

    /**
     * 工具 HTTP 地址。
     */
    private String httpUrl;

    /**
     * 工具 HTTP 方法。
     */
    private String httpMethod;

    /**
     * 工具 HTTP 头（JSON 字符串）。
     */
    private String httpHeaders;

    /**
     * 请求超时时间（毫秒）。
     */
    private Integer timeout;

    /**
     * 失败重试次数。
     */
    private Integer retryTimes;

    /**
     * 风险等级（LOW/MEDIUM/HIGH）。
     */
    private String riskLevel;

    /**
     * 状态（0-禁用，1-启用）。
     */
    private Integer status;

    /**
     * 最近调用摘要（时间 + 成功/失败）。
     */
    private String lastCallSummary;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
