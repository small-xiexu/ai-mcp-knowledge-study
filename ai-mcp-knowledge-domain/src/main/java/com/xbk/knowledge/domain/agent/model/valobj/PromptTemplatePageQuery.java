package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PromptTemplate 分页查询条件。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplatePageQuery {

    /**
     * 关键字。
     */
    private String keyword;

    /**
     * 状态。
     */
    private String state;

    /**
     * 分页偏移量。
     */
    private Integer offset;

    /**
     * 分页大小。
     */
    private Integer pageSize;
}
