package com.xbk.knowledge.domain.model.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PromptTemplate 按 ID 查询条件（含 org 边界）。
 
  * @author xiexu
  */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateIdQuery {

    private Long orgId;

    private Long id;
}

