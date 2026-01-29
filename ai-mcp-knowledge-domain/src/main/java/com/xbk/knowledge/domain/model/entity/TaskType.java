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
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务编码（唯一）
     */
    private String taskCode;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 首选模型ID
     */
    private Long preferredModelId;

    /**
     * 备用模型ID列表（逗号分隔）
     */
    private String fallbackModelIds;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
