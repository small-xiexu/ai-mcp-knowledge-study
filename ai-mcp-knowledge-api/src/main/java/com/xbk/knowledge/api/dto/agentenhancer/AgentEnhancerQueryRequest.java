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

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 关键字
     */
    private String keyword;

    /**
     * 启用状态
     */
    private Boolean enabled;

    /**
     * AgentEnhancer类型
     */
    private String agentEnhancerType;
}

