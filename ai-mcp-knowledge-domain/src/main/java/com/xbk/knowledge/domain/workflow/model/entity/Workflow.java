package com.xbk.knowledge.domain.workflow.model.entity;

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
 * Workflow 资产实体（独立于 Agent）。
 *
 * 对应表：workflow
 *
 * @author sxie
 */
@TableName("workflow")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String workflowCode;

    private String workflowName;

    private String description;

    /**
     * ENABLED/DISABLED。
     */
    private String status;

    private Long currentPublishedVersionId;

    private Long createdBy;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

