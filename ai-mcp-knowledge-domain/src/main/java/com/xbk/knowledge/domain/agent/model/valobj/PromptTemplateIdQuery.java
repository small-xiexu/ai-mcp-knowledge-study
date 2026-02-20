package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PromptTemplate 按 ID 查询条件。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateIdQuery {

    private Long id;
}
