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
     * 查询 Advisor 绑定关系列表。
     */
    List<AdvisorBinding> listBindings(AdvisorBindingQuery query);

    /**
     * 查询 Advisor 绑定视图列表（含运行时装配信息）。
     */
    List<AdvisorBindingView> listBindingViews(AdvisorBindingQuery query);

    /**
     * 删除指定绑定目标下的 Advisor 绑定。
     */
    int deleteByTarget(AdvisorBindingQuery query);

    /**
     * 新增 Advisor 绑定关系。
     */
    int insertBinding(AdvisorBinding binding);

    /**
     * 删除指定 Advisor 的全部绑定关系。
     */
    int deleteByAdvisorId(Long advisorId);
}
