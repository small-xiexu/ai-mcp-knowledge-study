package com.xbk.knowledge.domain.model.vo.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow 图查询条件（按 versionId）。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowGraphQuery {

    private Long orgId;

    private Long workflowVersionId;
}

