package com.xbk.knowledge.api.dto.tool;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具风险策略分页查询请求。
 
  * @author xiexu
  */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolPolicyQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 关键字（模糊匹配 toolKey）。
     */
    private String keyword;

    /**
     * 启用状态过滤（可空）。
     */
    private Boolean enabled;
}

