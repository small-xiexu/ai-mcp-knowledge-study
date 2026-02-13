package com.xbk.knowledge.domain.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统用户实体。
 * 对应数据库表：sys_user
 *
 * 职责：领域实体，用于承载用户身份状态。
 *
 * @author xiexu
 */
@TableName("sys_user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUser {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 密码哈希。
     */
    private String passwordHash;

    /**
     * 账号状态：1启用、0禁用、2锁定。
     */
    private Integer status;

    /**
     * 是否平台超管：1是、0否。
     */
    private Integer isSuperAdmin;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP。
     */
    private String lastLoginIp;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
