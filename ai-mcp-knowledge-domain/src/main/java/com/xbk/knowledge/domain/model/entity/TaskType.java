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
 * 任务类型实体
 * 对应数据库表：ai_task_type
 *
 * 职责：领域实体，用于承载核心业务状态与生命周期
 * @author xiexu
 */
@TableName("ai_task_type")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskType {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 组织ID。
     */
    private Long orgId;

    /**
     * 任务名称
     *
     * 为什么：展示与识别任务类型
     */
    private String taskName;

    /**
     * 任务编码（唯一）
     *
     * 为什么：用于程序化定位任务类型
     */
    private String taskCode;

    /**
     * 任务描述
     *
     * 为什么：补充业务语义说明
     */
    private String description;

    /**
     * 首选模型ID
     *
     * 为什么：用于推荐与路由
     */
    private Long preferredModelId;

    /**
     * 备用模型ID列表（逗号分隔）
     *
     * 为什么：主模型不可用时的降级选择
     */
    private String fallbackModelIds;

    /**
     * 创建时间
     *
     * 为什么：用于审计与排序
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 为什么：用于审计与变更追踪
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
