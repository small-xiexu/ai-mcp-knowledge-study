package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 调度分页查询参数。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentSchedulePageQuery {

    /**
     * Agent ID（可空）。
     */
    private Long agentId;

    /**
     * 调度名称（可空，模糊匹配）。
     */
    private String scheduleName;

    /**
     * 启用状态（可空）。
     */
    private Boolean enabled;

    /**
     * 偏移量。
     */
    private Integer offset;

    /**
     * 页大小。
     */
    private Integer pageSize;
}
