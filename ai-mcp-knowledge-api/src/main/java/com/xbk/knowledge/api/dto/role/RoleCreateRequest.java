package com.xbk.knowledge.api.dto.role;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 角色创建请求 DTO。
 *
 * 职责：接口层 DTO，用于承载角色创建参数。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleCreateRequest extends BaseRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 角色编码。
     */
    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z_][A-Z0-9_]{1,63}$", message = "角色编码格式不正确")
    private String roleCode;

    /**
     * 角色名称。
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 128, message = "角色名称长度不能超过128")
    private String roleName;

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
