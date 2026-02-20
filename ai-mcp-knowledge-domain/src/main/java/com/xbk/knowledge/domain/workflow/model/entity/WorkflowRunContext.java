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
 * Workflow 运行上下文快照（用于审批后续跑）。
 *
 * 对应表：workflow_run_context
 *
 * @author sxie
 */
@TableName("workflow_run_context")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunContext {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String runId;

    /**
     * SAVED/RESUMED/EXPIRED。
     */
    private String status;

    private String snapshotJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

