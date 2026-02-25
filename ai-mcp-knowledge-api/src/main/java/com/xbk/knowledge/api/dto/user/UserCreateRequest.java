package com.xbk.knowledge.api.dto.user;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户创建请求 DTO。
 *
 * 职责：接口层 DTO，用于承载新建用户参数。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserCreateRequest extends BaseRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 用户名。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度应在3到64之间")
    private String username;

    /**
     * 显示名。
     */
    @NotBlank(message = "显示名不能为空")
    @Size(max = 128, message = "显示名长度不能超过128")
    private String displayName;

    /**
     * 密码。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度应在8到64之间")
    private String password;

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
     * 账号状态1启用、0禁用、2锁定。
     */
    private Integer status;

    /**
     * 是否平台超管。
     */
    private Boolean superAdmin;
}
