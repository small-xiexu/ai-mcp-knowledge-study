package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 分页查询条件（含 scope 边界）。
 
  * @author xiexu
  */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentPageQuery {

    private String keyword;

    private String status;

    private Integer offset;

    private Integer pageSize;
}

