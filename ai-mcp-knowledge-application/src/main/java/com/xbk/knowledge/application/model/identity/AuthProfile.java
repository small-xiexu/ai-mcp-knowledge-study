package com.xbk.knowledge.application.model.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录用户画像模型。
 *
 * 职责：应用层模型，用于聚合身份与权限信息。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthProfile {

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
    private List<String> roleCodes;

    /**
     * 权限编码集合。
     */
    private List<String> permissionCodes;
}
