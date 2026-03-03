package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Gateway 凭证响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayAuthResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 凭证主键 ID。
     */
    private Long id;

    /**
     * 所属网关 ID。
     */
    private String gatewayId;

    /**
     * 鉴权 API Key。
     */
    private String apiKey;

    /**
     * 限流阈值（每分钟请求数）。
     */
    private Integer rateLimit;

    /**
     * 凭证过期时间。
     */
    private LocalDateTime expireTime;

    /**
     * 状态（0-禁用，1-启用）。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
