package com.xbk.knowledge.api.dto.user;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户更新请求 DTO。
 *
 * 职责：接口层 DTO，用于承载用户基础信息编辑参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserUpdateRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID。
     */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /**
     * 显示名。
     */
    @NotBlank(message = "显示名不能为空")
    @Size(max = 128, message = "显示名长度不能超过128")
    private String displayName;

    /**
     * 邮箱。
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    /**
     * 手机号。
     */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    /**
     * 账号状态：1启用、0禁用、2锁定。
     */
    @NotNull(message = "账号状态不能为空")
    @Min(value = 0, message = "账号状态不合法")
    @Max(value = 2, message = "账号状态不合法")
    private Integer status;

    /**
     * 是否平台超管。
     */
    private Boolean superAdmin;
}
