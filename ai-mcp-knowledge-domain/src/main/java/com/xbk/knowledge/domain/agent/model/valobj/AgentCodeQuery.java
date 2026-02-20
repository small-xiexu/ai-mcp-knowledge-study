package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 按 code 查询条件（含 scope 边界）。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCodeQuery {

    private String agentCode;
}

