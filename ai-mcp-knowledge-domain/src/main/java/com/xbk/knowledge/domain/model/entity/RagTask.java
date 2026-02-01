package com.xbk.knowledge.domain.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xbk.knowledge.types.enums.RagTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * RAG 任务实体
 * 对应数据库表：ai_rag_task
 *
 * 职责：记录知识库构建任务
 * @author xiexu
 */
@TableName("ai_rag_task")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagTask {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID（业务标识）
     */
    private String taskId;

    /**
     * 任务类型（FILE/GIT）
     */
    private String type;

    /**
     * 任务状态
     */
    private RagTaskStatus status;

    /**
     * 任务进度（0-100）
     */
    private Integer progress;

    /**
     * 状态描述
     */
    private String message;

    /**
     * 知识库标签
     */
    private String ragTag;

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
