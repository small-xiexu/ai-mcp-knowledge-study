package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 列表查询请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentQueryRequest extends PageRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 关键字（agentCode/agentName 模糊匹配）。
     */
    private String keyword;

    /**
     * ENABLED/DISABLED（可选）。
     */
    private String status;
}
