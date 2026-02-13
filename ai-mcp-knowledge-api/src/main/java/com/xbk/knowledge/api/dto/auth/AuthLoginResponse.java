package com.xbk.knowledge.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录响应 DTO。
 *
 * 职责：接口层 DTO，用于返回令牌与用户画像。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Token 名称。
     */
    private String tokenName;

    /**
     * Token 值。
     */
    private String tokenValue;

    /**
     * Token 剩余有效期（秒）。
     */
    private Long tokenTimeout;

    /**
     * 当前登录用户信息。
     */
    private AuthProfileResponse profile;
}
