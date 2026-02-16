package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Workflow 更新请求（按 id 更新）。
 
  * @author xiexu
  */
@Data
public class WorkflowUpdateRequest {

    @NotNull(message = "id 不能为空")
    private Long id;

    @NotBlank(message = "workflowName 不能为空")
    private String workflowName;

    private String description;

    /**
     * ENABLED/DISABLED
     */
    private String status;
}

