package com.xbk.knowledge.infrastructure.advisor.repository;

import com.xbk.knowledge.domain.advisor.model.entity.Advisor;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorPageQuery;
import com.xbk.knowledge.domain.advisor.adapter.repository.AdvisorRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IAdvisorDao;
import com.xbk.knowledge.infrastructure.dao.po.AdvisorPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Advisor 资产仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class AdvisorRepositoryImpl implements AdvisorRepository {

    private final IAdvisorDao mapper;

    /**
     * findById。
     *
     * @param scopeId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Optional<Advisor> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(id))
                .map(item -> BeanMappingUtils.map(item, Advisor.class));
    }

    /**
     * findByCode。
     *
     * @param scopeId 参数
     * @param advisorCode 参数
     * @return 返回结果
     */
    @Override
    public Optional<Advisor> findByCode(String advisorCode) {
        if (!StringUtils.hasText(advisorCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByCode(advisorCode))
                .map(item -> BeanMappingUtils.map(item, Advisor.class));
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<Advisor> findPage(AdvisorPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findPage(query), Advisor.class);
    }

    /**
     * count。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long count(AdvisorPageQuery query) {
        if (query == null) {
            return 0;
        }
        return mapper.count(query);
    }

    /**
     * insert。
     *
     * @param advisor 参数
     * @return 返回结果
     */
    @Override
    public Advisor insert(Advisor advisor) {
        if (advisor == null) {
            return null;
        }
        mapper.insertAdvisor(BeanMappingUtils.map(advisor, AdvisorPO.class));
        return advisor;
    }

    /**
     * update。
     *
     * @param advisor 参数
     * @return 返回结果
     */
    @Override
    public int update(Advisor advisor) {
        if (advisor == null || advisor.getId() == null) {
            return 0;
        }
        return mapper.updateAdvisor(BeanMappingUtils.map(advisor, AdvisorPO.class));
    }

    /**
     * updateEnabled。
     *
     * @param scopeId 参数
     * @param id 参数
     * @param enabled 参数
     * @return 返回结果
     */
    @Override
    public int updateEnabled(Long id, Integer enabled) {
        if (id == null || enabled == null) {
            return 0;
        }
        return mapper.updateEnabled(id, enabled);
    }

    /**
     * deleteById。
     *
     * @param scopeId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public int deleteById(Long id) {
        if (id == null) {
            return 0;
        }
        return mapper.deleteById(id);
    }
}
