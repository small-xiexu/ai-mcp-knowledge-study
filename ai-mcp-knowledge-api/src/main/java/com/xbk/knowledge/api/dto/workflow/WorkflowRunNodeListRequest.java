package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * WorkflowRunNodeList 请求参数模型。
 *
 * @author sxie
 */
@Data
public class WorkflowRunNodeListRequest {

    /**
     * 运行ID
     */
    @NotBlank(message = "runId 不能为空")
    private String runId;
}

