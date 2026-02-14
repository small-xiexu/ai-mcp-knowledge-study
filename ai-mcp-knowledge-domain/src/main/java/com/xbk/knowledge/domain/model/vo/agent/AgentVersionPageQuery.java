package com.xbk.knowledge.domain.model.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentVersion 分页查询条件（含 org 边界）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersionPageQuery {

    private Long orgId;

    private Long agentId;

    private Integer offset;

    private Integer pageSize;
}

