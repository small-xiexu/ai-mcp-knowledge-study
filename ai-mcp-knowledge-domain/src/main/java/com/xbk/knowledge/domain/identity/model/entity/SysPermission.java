package com.xbk.knowledge.domain.identity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统权限实体。
 * 对应数据库表：sys_permission
 *
 * 职责：领域实体，用于承载权限定义。
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysPermission {

    /**
     * 主键ID。
     */
    private Long id;

    /**
     * 权限编码。
     */
    private String permissionCode;

    /**
     * 权限名称。
     */
    private String permissionName;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 动作。
     */
    private String action;

    /**
     * 状态：1启用、0禁用。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
