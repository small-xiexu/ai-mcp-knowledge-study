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
     * 按主键查询 Advisor 配置。
     *
     * @param id Advisor 主键
     * @return Advisor 配置（不存在时返回空）
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
     * 按编码查询 Advisor 配置。
     *
     * @param advisorCode Advisor 业务编码
     * @return Advisor 配置（不存在时返回空）
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
     * 分页查询 Advisor 列表。
     *
     * @param query 分页与筛选条件
     * @return Advisor 分页数据列表
     */
    @Override
    public List<Advisor> findPage(AdvisorPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findPage(query), Advisor.class);
    }

    /**
     * 统计 Advisor 查询结果总数。
     *
     * @param query 分页与筛选条件
     * @return 命中总数
     */
    @Override
    public long count(AdvisorPageQuery query) {
        if (query == null) {
            return 0;
        }
        return mapper.count(query);
    }

    /**
     * 新增 Advisor 配置。
     *
     * @param advisor Advisor 领域实体
     * @return 新增后的 Advisor 领域实体
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
     * 更新 Advisor 配置。
     *
     * @param advisor Advisor 领域实体
     * @return 更新影响行数
     */
    @Override
    public int update(Advisor advisor) {
        if (advisor == null || advisor.getId() == null) {
            return 0;
        }
        return mapper.updateAdvisor(BeanMappingUtils.map(advisor, AdvisorPO.class));
    }

    /**
     * 更新 Advisor 启用状态。
     *
     * @param id Advisor 主键
     * @param enabled 启用状态（1 启用，0 禁用）
     * @return 更新影响行数
     */
    @Override
    public int updateEnabled(Long id, Integer enabled) {
        if (id == null || enabled == null) {
            return 0;
        }
        return mapper.updateEnabled(id, enabled);
    }

    /**
     * 按主键删除 Advisor 配置。
     *
     * @param id Advisor 主键
     * @return 删除影响行数
     */
    @Override
    public int deleteById(Long id) {
        if (id == null) {
            return 0;
        }
        return mapper.deleteById(id);
    }
}
