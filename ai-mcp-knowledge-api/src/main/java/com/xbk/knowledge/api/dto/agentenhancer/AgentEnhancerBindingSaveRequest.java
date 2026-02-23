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

    /**
     * 绑定类型
     */
    @NotBlank(message = "bindType 不能为空")
    private String bindType;

    /**
     * 绑定目标ID
     */
    @NotNull(message = "bindTargetId 不能为空")
    private Long bindTargetId;

    /**
     * items
     */
    private List<AgentEnhancerBindingSaveItem> items;

    @Data
    public static class AgentEnhancerBindingSaveItem {
        /**
         * AgentEnhancer ID
         */
        @NotNull(message = "agentEnhancerId 不能为空")
        private Long agentEnhancerId;
        /**
         * 顺序号
         */
        private Integer orderNo;
        /**
         * 启用状态
         */
        private Boolean enabled;
    }
}

