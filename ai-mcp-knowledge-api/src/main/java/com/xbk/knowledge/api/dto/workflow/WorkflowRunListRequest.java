package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * WorkflowRunListRequest。
 *
 * @author sxie
 */
@Data
public class WorkflowRunListRequest {

    private String status;

    @Min(value = 0, message = "offset 不能小于 0")
    private Integer offset = 0;

    @Min(value = 1, message = "pageSize 不能小于 1")
    private Integer pageSize = 20;
}

