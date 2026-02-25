package com.xbk.knowledge.domain.agentenhancer.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentEnhancer 绑定查询条件。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentEnhancerBindingQuery {

    /**
     * 绑定类型 AGENT_VERSION/WORKFLOW_VERSION。
     */
    private String bindType;

    /**
     * 绑定目标 ID。
     */
    private Long bindTargetId;

    /**
     * 兼容原 record 访问方式。
     *
     * @return 绑定类型
     */
    public String bindType() {
        return bindType;
    }

    /**
     * 兼容原 record 访问方式。
     *
     * @return 绑定目标 ID
     */
    public Long bindTargetId() {
        return bindTargetId;
    }
}
