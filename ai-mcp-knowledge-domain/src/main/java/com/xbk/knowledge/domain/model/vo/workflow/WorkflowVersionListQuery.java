package com.xbk.knowledge.domain.model.vo.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WorkflowVersion 列表查询条件（按 workflowId）。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersionListQuery {

    private Long orgId;

    private Long workflowId;
}

