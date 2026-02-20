package com.xbk.knowledge.api.dto.auth;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO。
 *
 * 职责：接口层 DTO，用于承载认证入参。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthLoginRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码明文。
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
