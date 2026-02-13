package com.xbk.knowledge.api.dto.role;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 角色更新请求 DTO。
 *
 * 职责：接口层 DTO，用于承载角色更新参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleUpdateRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID。
     */
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /**
     * 角色名称。
     */
    @NotNull(message = "角色名称不能为空")
    @Size(max = 128, message = "角色名称长度不能超过128")
    private String roleName;

    /**
     * 角色范围。
     */
    private String roleScope;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 备注。
     */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
