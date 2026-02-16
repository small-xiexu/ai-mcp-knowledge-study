package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Workflow 列表请求。
 
  * @author xiexu
  */
@Data
public class WorkflowListRequest {

    private String keyword;

    @Min(value = 0, message = "offset 不能小于 0")
    private Integer offset = 0;

    @Min(value = 1, message = "pageSize 不能小于 1")
    private Integer pageSize = 20;
}

