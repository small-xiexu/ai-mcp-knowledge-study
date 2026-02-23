package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * WorkflowRunList 请求参数模型。
 *
 * @author sxie
 */
@Data
public class WorkflowRunListRequest {

    /**
     * 状态
     */
    private String status;

    /**
     * 偏移量
     */
    @Min(value = 0, message = "offset 不能小于 0")
    private Integer offset = 0;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "pageSize 不能小于 1")
    private Integer pageSize = 20;
}

