package com.xbk.knowledge.domain.advisor.adapter.repository;

import com.xbk.knowledge.domain.advisor.model.entity.AdvisorBinding;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingQuery;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingView;

import java.util.List;

/**
 * Advisor 绑定仓储接口。
 *
 * 职责：
 * - 按 bindType/bindTargetId 管理 advisor 绑定与排序
 * - 提供运行时装配需要的 join 视图查询
 *
 * @author sxie
 */
public interface AdvisorBindingRepository {

    /**
     * 方法：listBindings。
     */
    List<AdvisorBinding> listBindings(AdvisorBindingQuery query);

    /**
     * 方法：listBindingViews。
     */
    List<AdvisorBindingView> listBindingViews(AdvisorBindingQuery query);

    /**
     * 方法：deleteByTarget。
     */
    int deleteByTarget(AdvisorBindingQuery query);

    /**
     * 方法：insertBinding。
     */
    int insertBinding(AdvisorBinding binding);

    /**
     * 方法：deleteByAdvisorId。
     */
    int deleteByAdvisorId(Long advisorId);
}
