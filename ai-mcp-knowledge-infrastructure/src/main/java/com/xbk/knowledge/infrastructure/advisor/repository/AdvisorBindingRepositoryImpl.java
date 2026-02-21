package com.xbk.knowledge.infrastructure.advisor.repository;

import com.xbk.knowledge.domain.advisor.model.entity.AdvisorBinding;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingQuery;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingView;
import com.xbk.knowledge.domain.advisor.adapter.repository.AdvisorBindingRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IAdvisorBindingDao;
import com.xbk.knowledge.infrastructure.dao.po.AdvisorBindingPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Advisor 绑定仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class AdvisorBindingRepositoryImpl implements AdvisorBindingRepository {

    private final IAdvisorBindingDao mapper;

    /**
     * listBindings。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<AdvisorBinding> listBindings(AdvisorBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.listBindings(query), AdvisorBinding.class);
    }

    /**
     * listBindingViews。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<AdvisorBindingView> listBindingViews(AdvisorBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
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
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
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
        if (binding == null || !StringUtils.hasText(binding.getBindType())
                || binding.getBindTargetId() == null || binding.getAdvisorId() == null) {
            return 0;
        }
        return mapper.insertBinding(BeanMappingUtils.map(binding, AdvisorBindingPO.class));
    }

    /**
     * deleteByAdvisorId。
     *
     * @param advisorId 参数
     * @return 返回结果
     */
    @Override
    public int deleteByAdvisorId(Long advisorId) {
        if (advisorId == null) {
            return 0;
        }
        return mapper.deleteByAdvisorId(advisorId);
    }
}
