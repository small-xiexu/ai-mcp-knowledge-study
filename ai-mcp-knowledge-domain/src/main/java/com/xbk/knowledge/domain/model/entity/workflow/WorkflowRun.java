package com.xbk.knowledge.domain.model.entity.workflow;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * WorkflowRun 实体。
 *
 * 对应表：workflow_run
 
  * @author xiexu
  */
@TableName("workflow_run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRun {

    @TableId(value = "run_id")
    private String runId;

    private Long orgId;

    private Long workflowId;

    private String workflowCode;

    private Long workflowVersionId;

    private String triggerSource;

    private Long operatorId;

    private String operatorType;

    private Long sessionId;

    /**
     * RUNNING/SUCCESS/FAILED/PENDING_APPROVAL/CANCELLED。
     */
    private String status;

    private String currentNodeKey;

    private Long costMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

