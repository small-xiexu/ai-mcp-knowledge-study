package com.xbk.knowledge.domain.workflow.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow 按 code 查询条件（含 scope 边界）。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCodeQuery {

    private String workflowCode;
}

