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
    private Long id;

    private Long workflowId;

    private Integer versionNo;

    /**
     * DRAFT/PUBLISHED/ARCHIVED。
     */
    private String state;

    private String changeSummary;

    /**
     * 画布快照（nodes+edges+viewport），用于前端回显与审计回放。
     */
    private String graphJson;

    /**
     * 默认配置（JSON），节点可继承并覆盖。
     */
    private String defaultConfigJson;

    private Long createdBy;

    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

