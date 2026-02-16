package com.xbk.knowledge.infrastructure.repository.advisor;

import com.xbk.knowledge.domain.model.entity.advisor.Advisor;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorPageQuery;
import com.xbk.knowledge.domain.repository.advisor.AdvisorRepository;
import com.xbk.knowledge.infrastructure.mapper.advisor.AdvisorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Advisor 资产仓储实现。
 
  * @author xiexu
  */
@Repository
@RequiredArgsConstructor
public class AdvisorRepositoryImpl implements AdvisorRepository {

    private final AdvisorMapper mapper;

    /**
     * findById。
     *
     * @param orgId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Optional<Advisor> findById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(orgId, id));
    }

    /**
     * findByCode。
     *
     * @param orgId 参数
     * @param advisorCode 参数
     * @return 返回结果
     */
    @Override
    public Optional<Advisor> findByCode(Long orgId, String advisorCode) {
        if (orgId == null || !StringUtils.hasText(advisorCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByCode(orgId, advisorCode));
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<Advisor> findPage(AdvisorPageQuery query) {
        if (query == null || query.orgId() == null) {
            return Collections.emptyList();
        }
        return mapper.findPage(query);
    }

    /**
     * count。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long count(AdvisorPageQuery query) {
        if (query == null || query.orgId() == null) {
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
        mapper.insertAdvisor(advisor);
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
        if (advisor == null || advisor.getOrgId() == null || advisor.getId() == null) {
            return 0;
        }
        return mapper.updateAdvisor(advisor);
    }

    /**
     * updateEnabled。
     *
     * @param orgId 参数
     * @param id 参数
     * @param enabled 参数
     * @return 返回结果
     */
    @Override
    public int updateEnabled(Long orgId, Long id, Integer enabled) {
        if (orgId == null || id == null || enabled == null) {
            return 0;
        }
        return mapper.updateEnabled(orgId, id, enabled);
    }

    /**
     * deleteById。
     *
     * @param orgId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public int deleteById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return 0;
        }
        return mapper.deleteById(orgId, id);
    }
}

