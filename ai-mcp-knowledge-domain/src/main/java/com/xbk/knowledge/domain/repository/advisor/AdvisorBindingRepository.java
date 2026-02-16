package com.xbk.knowledge.domain.repository.advisor;

import com.xbk.knowledge.domain.model.entity.advisor.AdvisorBinding;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorBindingQuery;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorBindingView;

import java.util.List;

/**
 * Advisor 绑定仓储接口。
 *
 * 职责：
 * - 按 bindType/bindTargetId 管理 advisor 绑定与排序
 * - 提供运行时装配需要的 join 视图查询
 
  * @author xiexu
  */
public interface AdvisorBindingRepository {

    List<AdvisorBinding> listBindings(AdvisorBindingQuery query);

    List<AdvisorBindingView> listBindingViews(AdvisorBindingQuery query);

    int deleteByTarget(AdvisorBindingQuery query);

    int insertBinding(AdvisorBinding binding);
}

