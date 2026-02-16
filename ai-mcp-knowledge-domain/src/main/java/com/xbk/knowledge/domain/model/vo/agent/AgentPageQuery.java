package com.xbk.knowledge.domain.model.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 分页查询条件（含 org 边界）。
 
  * @author xiexu
  */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentPageQuery {

    private Long orgId;

    private String keyword;

    private String status;

    private Integer offset;

    private Integer pageSize;
}

