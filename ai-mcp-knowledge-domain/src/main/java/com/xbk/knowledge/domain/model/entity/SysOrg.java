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
 * 系统组织实体。
 * 对应数据库表：sys_org
 *
 * 职责：领域实体，用于承载组织结构信息。
 *
 * @author xiexu
 */
@TableName("sys_org")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysOrg {

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
     * 组织编码。
     */
    private String orgCode;

    /**
     * 组织名称。
     */
    private String orgName;

    /**
     * 父组织ID。
     */
    private Long parentId;

    /**
     * 组织路径。
     */
    private String orgPath;

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
