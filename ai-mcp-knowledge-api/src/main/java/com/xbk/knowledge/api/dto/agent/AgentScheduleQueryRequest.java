package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AgentSchedule 列表查询请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentScheduleQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 过滤：agentCode（可选）。
     */
    private String agentCode;

    /**
     * 过滤：调度名称（可选，模糊匹配）。
     */
    private String scheduleName;

    /**
     * 过滤：启用状态（可选）。
     */
    private Boolean enabled;
}
