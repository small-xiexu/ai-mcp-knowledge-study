package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PromptTemplate 列表查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptTemplateQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 关键字（templateCode/templateName 模糊匹配）。
     */
    private String keyword;

    /**
     * scope：GLOBAL/ORG（可选）。
     */
    private String scope;

    /**
     * state：DRAFT/PUBLISHED/ARCHIVED（可选）。
     */
    private String state;
}
