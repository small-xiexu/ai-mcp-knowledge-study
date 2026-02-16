package com.xbk.knowledge.domain.model.vo.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow 按 code 查询条件（含 org 边界）。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCodeQuery {

    private Long orgId;

    private String workflowCode;
}

