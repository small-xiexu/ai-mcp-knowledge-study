package com.xbk.knowledge.domain.model.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 按 code 查询条件（含 org 边界）。
 
  * @author xiexu
  */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCodeQuery {

    private Long orgId;

    private String agentCode;
}

