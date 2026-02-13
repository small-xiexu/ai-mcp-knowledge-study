package com.xbk.knowledge.api.dto.apikey;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * API Key 禁用请求 DTO。
 *
 * 职责：接口层 DTO，用于承载 API Key 禁用参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApiKeyRevokeRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * API Key ID。
     */
    @NotNull(message = "API Key ID不能为空")
    private Long id;

    /**
     * 租户ID（仅超管可指定）。
     */
    private String tenantId;
}
