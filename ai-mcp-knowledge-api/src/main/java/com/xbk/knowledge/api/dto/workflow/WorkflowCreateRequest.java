package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Workflow 创建请求。
 *
 * @author sxie
 */
@Data
public class WorkflowCreateRequest {

    @NotBlank(message = "workflowCode 不能为空")
    private String workflowCode;

    @NotBlank(message = "workflowName 不能为空")
    private String workflowName;

    private String description;
}

