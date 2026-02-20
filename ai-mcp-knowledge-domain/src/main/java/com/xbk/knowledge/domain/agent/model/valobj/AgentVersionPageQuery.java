package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentVersion 分页查询条件（含 scope 边界）。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersionPageQuery {

    private Long agentId;

    private Integer offset;

    private Integer pageSize;
}

