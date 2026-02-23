package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Workflow 列表请求。
 *
 * @author sxie
 */
@Data
public class WorkflowListRequest {

    /**
     * 关键字
     */
    private String keyword;

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

