package com.xbk.knowledge.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户响应 DTO。
 *
 * 职责：接口层 DTO，用于输出用户基础信息。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID。
     */
    private Long id;

    /**
     * 租户ID。
     */
    private String tenantId;

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
     * 账号状态。
     */
    private Integer status;

    /**
     * 是否超管。
     */
    private Boolean superAdmin;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
