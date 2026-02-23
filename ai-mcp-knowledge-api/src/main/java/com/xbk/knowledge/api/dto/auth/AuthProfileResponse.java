package com.xbk.knowledge.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户画像响应 DTO。
 *
 * 职责：接口层 DTO，用于返回身份与权限信息。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthProfileResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 显示名。
     */
    private String displayName;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 手机号。
     */
    private String mobile;

    /**
     * 是否平台超管。
     */
    private Boolean superAdmin;

    /**
     * 角色编码集合。
     */
    private List<String> roles;

    /**
     * 权限编码集合。
     */
    private List<String> permissions;
}
