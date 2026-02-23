package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Workflow 创建参数。
 *
 * @author sxie
 */
@Data
public class WorkflowCreateRequest {

    /**
     * Workflow 编码
     */
    @NotBlank(message = "workflowCode 不能为空")
    private String workflowCode;

    /**
     * Workflow 名称
     */
    @NotBlank(message = "workflowName 不能为空")
    private String workflowName;

    /**
     * 描述
     */
    private String description;
}

