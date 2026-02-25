package com.xbk.knowledge.domain.workflow.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow 按 code 查询条件。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCodeQuery {

    /**
     * Workflow 编码。
     */
    private String workflowCode;
}
