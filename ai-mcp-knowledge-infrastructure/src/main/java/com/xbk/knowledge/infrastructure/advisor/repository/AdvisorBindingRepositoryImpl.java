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
     * 查询指定绑定目标下的 Advisor 绑定关系。
     *
     * @param query 绑定查询条件（绑定类型 + 绑定目标 ID）
     * @return 绑定关系列表
     */
    @Override
    public List<AdvisorBinding> listBindings(AdvisorBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.listBindings(query), AdvisorBinding.class);
    }

    /**
     * 查询绑定视图数据（含 Advisor 基础信息）。
     *
     * @param query 绑定查询条件（绑定类型 + 绑定目标 ID）
     * @return 绑定视图列表
     */
    @Override
    public List<AdvisorBindingView> listBindingViews(AdvisorBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return Collections.emptyList();
        }
        return mapper.listBindingViews(query);
    }

    /**
     * 删除指定绑定目标下的全部 Advisor 绑定关系。
     *
     * @param query 绑定查询条件（绑定类型 + 绑定目标 ID）
     * @return 删除影响行数
     */
    @Override
    public int deleteByTarget(AdvisorBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            return 0;
        }
        return mapper.deleteByTarget(query);
    }

    /**
     * 新增一条 Advisor 绑定关系。
     *
     * @param binding 绑定实体
     * @return 新增影响行数
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
     * 按 Advisor 主键删除其全部绑定关系。
     *
     * @param advisorId Advisor 主键
     * @return 删除影响行数
     */
    @Override
    public int deleteByAdvisorId(Long advisorId) {
        if (advisorId == null) {
            return 0;
        }
        return mapper.deleteByAdvisorId(advisorId);
    }
}
