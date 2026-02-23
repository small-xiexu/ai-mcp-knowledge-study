package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Workflow 更新参数（按 id 更新）。
 *
 * @author sxie
 */
@Data
public class WorkflowUpdateRequest {

    /**
     * 主键ID
     */
    @NotNull(message = "id 不能为空")
    private Long id;

    /**
     * Workflow 名称
     */
    @NotBlank(message = "workflowName 不能为空")
    private String workflowName;

    /**
     * 描述
     */
    private String description;

    /**
     * ENABLED/DISABLED
     */
    private String status;
}

