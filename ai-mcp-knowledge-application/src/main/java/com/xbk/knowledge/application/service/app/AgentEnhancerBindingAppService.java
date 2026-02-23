package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingQuery;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingView;

import java.util.List;

/**
 * AgentEnhancer 绑定控制面应用服务。
 *
 * @author sxie
 */
public interface AgentEnhancerBindingAppService {

    List<AgentEnhancerBindingView> listBindings(AgentEnhancerBindingQuery query);

    void saveBindings(String bindType, Long bindTargetId, List<AgentEnhancerBindingSaveItem> items);

    /**
     * 绑定保存条目（用于排序/启停）。
     */
    final class AgentEnhancerBindingSaveItem {
        private Long agentEnhancerId;
        private Integer orderNo;
        private Boolean enabled;

        /**
         * 获取 Agent 增强器 ID。
         *
         * @return 返回 Agent 增强器 ID。
         */
        public Long getAgentEnhancerId() {
            return agentEnhancerId;
        }

        /**
         * 设置 Agent 增强器 ID。
         *
         * @param agentEnhancerId Agent 增强器 ID。
         */
        public void setAgentEnhancerId(Long agentEnhancerId) {
            this.agentEnhancerId = agentEnhancerId;
        }

        /**
         * 获取绑定排序号。
         *
         * @return 返回绑定排序号。
         */
        public Integer getOrderNo() {
            return orderNo;
        }

        /**
         * 设置绑定排序号。
         *
         * @param orderNo 排序号。
         */
        public void setOrderNo(Integer orderNo) {
            this.orderNo = orderNo;
        }

        /**
         * 获取绑定启用状态。
         *
         * @return 返回是否启用。
         */
        public Boolean getEnabled() {
            return enabled;
        }

        /**
         * 设置绑定启用状态。
         *
         * @param enabled 启用状态
         */
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
