package com.xbk.knowledge.api.dto.apikey;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 创建请求 DTO。
 *
 * 职责：接口层 DTO，用于承载 API Key 创建参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApiKeyCreateRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID（仅超管可指定）。
     */
    private String tenantId;

    /**
     * 归属用户ID（默认当前用户）。
     */
    private Long ownerUserId;

    /**
     * 权限范围列表。
     */
    private List<String> scopes;

    /**
     * 过期时间。
     */
    private LocalDateTime expireAt;
}
