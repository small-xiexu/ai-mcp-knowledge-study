package com.xbk.knowledge.api.dto.apikey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API Key 创建响应 DTO。
 *
 * 职责：接口层 DTO，用于输出 API Key 创建结果（含一次性密钥）。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyCreateResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID。
     */
    private Long id;

    /**
     * 租户ID。
     */
    private String tenantId;

    /**
     * 归属用户ID。
     */
    private Long ownerUserId;

    /**
     * 访问Key。
     */
    private String accessKey;

    /**
     * 权限范围(JSON)。
     */
    private String scopes;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 过期时间。
     */
    private LocalDateTime expireAt;

    /**
     * 最后使用时间。
     */
    private LocalDateTime lastUsedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 一次性密钥明文。
     */
    private String secret;
}
