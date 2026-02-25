package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Workflow 运行上下文快照（用于审批后续跑）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunContext {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 运行 ID。
     */
    private String runId;

    /**
     * SAVED/RESUMED/EXPIRED。
     */
    private String status;

    /**
     * 运行快照 JSON。
     */
    private String snapshotJson;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
