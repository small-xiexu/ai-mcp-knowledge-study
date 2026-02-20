package com.xbk.knowledge.api.dto.workflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WorkflowResponse。
 *
 * @author sxie
 */
@Data
@Builder
public class WorkflowResponse {

    private Long id;
    private String workflowCode;
    private String workflowName;
    private String description;
    private String status;
    private Long currentPublishedVersionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
