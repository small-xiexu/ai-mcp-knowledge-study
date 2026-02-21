package com.xbk.knowledge.infrastructure.dao.po;

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
 * 系统角色实体。
 * 对应数据库表：sys_role
 *
 * 职责：领域实体，用于承载角色信息。
 *
 * @author sxie
 */
@TableName("sys_role")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysRolePO {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码。
     */
    private String roleCode;

    /**
     * 角色名称。
     */
    private String roleName;

    /**
     * 状态：1启用、0禁用。
     */
    private Integer status;

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
