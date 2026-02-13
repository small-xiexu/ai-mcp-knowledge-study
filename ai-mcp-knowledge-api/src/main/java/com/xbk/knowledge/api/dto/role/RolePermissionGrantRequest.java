package com.xbk.knowledge.api.dto.role;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 角色权限绑定请求 DTO。
 *
 * 职责：接口层 DTO，用于承载角色授权参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RolePermissionGrantRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID。
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 权限ID列表。
     */
    private List<Long> permissionIds;
}
