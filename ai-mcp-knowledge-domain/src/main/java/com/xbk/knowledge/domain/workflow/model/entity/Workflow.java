package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Workflow 资产实体（独立于 Agent）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * Workflow 编码。
     */
    private String workflowCode;

    /**
     * Workflow 名称。
     */
    private String workflowName;

    /**
     * Workflow 描述。
     */
    private String description;

    /**
     * ENABLED/DISABLED。
     */
    private String status;

    /**
     * 当前发布版本 ID。
     */
    private Long currentPublishedVersionId;

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
