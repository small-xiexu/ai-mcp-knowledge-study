package com.xbk.knowledge.infrastructure.repository.advisor;

import com.xbk.knowledge.domain.model.entity.advisor.AdvisorBinding;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorBindingQuery;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorBindingView;
import com.xbk.knowledge.domain.repository.advisor.AdvisorBindingRepository;
import com.xbk.knowledge.infrastructure.mapper.advisor.AdvisorBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Advisor 绑定仓储实现。
 
  * @author xiexu
  */
@Repository
@RequiredArgsConstructor
public class AdvisorBindingRepositoryImpl implements AdvisorBindingRepository {

    private final AdvisorBindingMapper mapper;

    /**
     * listBindings。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<AdvisorBinding> listBindings(AdvisorBindingQuery query) {
        if (query == null || query.orgId() == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return Collections.emptyList();
        }
        return mapper.listBindings(query);
    }

    /**
     * listBindingViews。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<AdvisorBindingView> listBindingViews(AdvisorBindingQuery query) {
        if (query == null || query.orgId() == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return Collections.emptyList();
        }
        return mapper.listBindingViews(query);
    }

    /**
     * deleteByTarget。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public int deleteByTarget(AdvisorBindingQuery query) {
        if (query == null || query.orgId() == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return 0;
        }
        return mapper.deleteByTarget(query);
    }

    /**
     * insertBinding。
     *
     * @param binding 参数
     * @return 返回结果
     */
    @Override
    public int insertBinding(AdvisorBinding binding) {
        if (binding == null || binding.getOrgId() == null || !StringUtils.hasText(binding.getBindType())
                || binding.getBindTargetId() == null || binding.getAdvisorId() == null) {
            return 0;
        }
        return mapper.insertBinding(binding);
    }
}

