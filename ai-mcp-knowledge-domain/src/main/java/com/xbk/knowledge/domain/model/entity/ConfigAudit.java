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
 * 配置审计实体
 * 对应数据库表：ai_config_audit
 *
 * 职责：领域实体，用于承载核心业务状态与生命周期
 * @author xiexu
 */
@TableName("ai_config_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigAudit {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 记录ID
     */
    private Long recordId;

    /**
     * 操作类型（INSERT/UPDATE/DELETE）
     */
    private String operation;

    /**
     * 旧值（JSON格式）
     */
    private String oldValue;

    /**
     * 新值（JSON格式）
     */
    private String newValue;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
