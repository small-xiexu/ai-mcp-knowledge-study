package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * WorkflowRun 按 runId 查询参数。
 *
 * @author sxie
 */
@Data
public class WorkflowRunGetRequest {

    /**
     * 运行ID
     */
    @NotBlank(message = "runId 不能为空")
    private String runId;
}

