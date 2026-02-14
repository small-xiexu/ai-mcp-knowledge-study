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
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 组织ID（资源归属 org）。
     */
    private Long orgId;

    /**
     * 表名
     *
     * 为什么：定位被审计的业务表
     */
    private String tableName;

    /**
     * 记录ID
     *
     * 为什么：定位被审计的记录
     */
    private Long recordId;

    /**
     * 操作类型（INSERT/UPDATE/DELETE）
     *
     * 为什么：标识变更类型
     */
    private String operation;

    /**
     * 旧值（JSON格式）
     *
     * 为什么：保留变更前状态用于追溯
     */
    private String oldValue;

    /**
     * 新值（JSON格式）
     *
     * 为什么：保留变更后状态用于对比
     */
    private String newValue;

    /**
     * 操作人
     *
     * 为什么：记录变更责任人
     */
    private String operator;

    /**
     * 创建时间
     *
     * 为什么：用于时序分析与审计
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
