package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * WorkflowRun 按 runId 查询请求。
 
  * @author xiexu
  */
@Data
public class WorkflowRunGetRequest {

    @NotBlank(message = "runId 不能为空")
    private String runId;
}

