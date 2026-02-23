package com.xbk.knowledge.api.dto.agentenhancer;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AgentEnhancer 分页查询参数。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentEnhancerQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    private String keyword;

    private Boolean enabled;

    private String agentEnhancerType;
}

