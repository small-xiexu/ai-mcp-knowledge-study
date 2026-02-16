package com.xbk.knowledge.domain.model.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PromptTemplate 分页查询条件（含 org 边界）。
 *
 * 说明：列表默认返回（GLOBAL + 当前 org 的 ORG 模板）。
 
  * @author xiexu
  */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplatePageQuery {

    private Long orgId;

    private String keyword;

    private String scope;

    private String state;

    private Integer offset;

    private Integer pageSize;
}

