package com.xbk.knowledge.api.dto.workflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WorkflowVersionResponse。
 *
 * @author xiexu
 */
@Data
@Builder
public class WorkflowVersionResponse {

    private Long id;
    private Long workflowId;
    private Integer versionNo;
    private String state;
    private String changeSummary;
    private String graphJson;
    private String defaultConfigJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
