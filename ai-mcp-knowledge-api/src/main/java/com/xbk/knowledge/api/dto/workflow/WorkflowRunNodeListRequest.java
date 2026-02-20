package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * WorkflowRunNodeListRequest。
 *
 * @author sxie
 */
@Data
public class WorkflowRunNodeListRequest {

    @NotBlank(message = "runId 不能为空")
    private String runId;
}

