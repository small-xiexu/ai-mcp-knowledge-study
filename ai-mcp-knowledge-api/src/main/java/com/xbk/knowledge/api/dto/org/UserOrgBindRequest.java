package com.xbk.knowledge.api.dto.org;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 用户组织绑定请求 DTO。
 *
 * 职责：接口层 DTO，用于承载用户组织绑定参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserOrgBindRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID。
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 组织ID。
     */
    @NotNull(message = "组织ID不能为空")
    private Long orgId;
}
