package com.xbk.knowledge.domain.model.vo.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WorkflowVersion 按 ID 查询条件（含 org 边界）。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersionIdQuery {

    private Long orgId;

    private Long id;
}

