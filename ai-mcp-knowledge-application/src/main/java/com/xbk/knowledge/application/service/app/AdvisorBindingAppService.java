package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingQuery;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingView;

import java.util.List;

/**
 * Advisor 绑定控制面应用服务。
 
  * @author xiexu
  */
public interface AdvisorBindingAppService {

    List<AdvisorBindingView> listBindings(AdvisorBindingQuery query);

    void saveBindings(String bindType, Long bindTargetId, List<AdvisorBindingSaveItem> items);

    /**
     * 绑定保存条目（用于排序/启停）。
     */
    final class AdvisorBindingSaveItem {
        private Long advisorId;
        private Integer orderNo;
        private Boolean enabled;

        /**
         * getAdvisorId。
         *
         * @return 返回结果
         */
        public Long getAdvisorId() {
            return advisorId;
        }

        /**
         * setAdvisorId。
         *
         * @param advisorId 参数
         */
        public void setAdvisorId(Long advisorId) {
            this.advisorId = advisorId;
        }

        /**
         * getOrderNo。
         *
         * @return 返回结果
         */
        public Integer getOrderNo() {
            return orderNo;
        }

        /**
         * setOrderNo。
         *
         * @param orderNo 参数
         */
        public void setOrderNo(Integer orderNo) {
            this.orderNo = orderNo;
        }

        /**
         * getEnabled。
         *
         * @return 返回结果
         */
        public Boolean getEnabled() {
            return enabled;
        }

        /**
         * setEnabled。
         *
         * @param enabled 参数
         */
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}

