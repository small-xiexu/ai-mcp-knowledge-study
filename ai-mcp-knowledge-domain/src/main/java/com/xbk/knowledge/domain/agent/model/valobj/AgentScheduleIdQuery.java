package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 调度按 ID 查询参数。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentScheduleIdQuery {

    /**
     * 调度ID。
     */
    private Long id;

    /**
     * 兼容原 record 访问方式。
     *
     * @return 调度ID
     */
    public Long id() {
        return id;
    }
}
