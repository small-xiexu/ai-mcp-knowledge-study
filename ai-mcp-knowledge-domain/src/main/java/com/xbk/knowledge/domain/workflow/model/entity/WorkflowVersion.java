package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * WorkflowVersion 实体（草稿/发布/历史）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersion {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * Workflow ID。
     */
    private Long workflowId;

    /**
     * 版本号。
     */
    private Integer versionNo;

    /**
     * DRAFT/PUBLISHED/ARCHIVED。
     */
    private String state;

    /**
     * 变更摘要。
     */
    private String changeSummary;

    /**
     * 画布快照（nodes+edges+viewport），用于前端回显与审计回放。
     */
    private String graphJson;

    /**
     * 默认配置（JSON），节点可继承并覆盖。
     */
    private String defaultConfigJson;

    /**
     * 创建人 ID。
     */
    private Long createdBy;

    /**
     * 更新人 ID。
     */
    private Long updatedBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
