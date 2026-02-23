package com.xbk.knowledge.api.dto.agentenhancer;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * AgentEnhancer 绑定保存请求。
 *
 * @author sxie
 */
@Data
public class AgentEnhancerBindingSaveRequest {

    @NotBlank(message = "bindType 不能为空")
    private String bindType;

    @NotNull(message = "bindTargetId 不能为空")
    private Long bindTargetId;

    private List<AgentEnhancerBindingSaveItem> items;

    @Data
    public static class AgentEnhancerBindingSaveItem {
        @NotNull(message = "agentEnhancerId 不能为空")
        private Long agentEnhancerId;
        private Integer orderNo;
        private Boolean enabled;
    }
}

