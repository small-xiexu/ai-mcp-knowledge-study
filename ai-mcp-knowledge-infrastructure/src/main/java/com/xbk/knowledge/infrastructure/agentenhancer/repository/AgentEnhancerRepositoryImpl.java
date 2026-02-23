package com.xbk.knowledge.infrastructure.agentenhancer.repository;

import com.xbk.knowledge.domain.agentenhancer.model.entity.AgentEnhancer;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerPageQuery;
import com.xbk.knowledge.domain.agentenhancer.adapter.repository.AgentEnhancerRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IAgentEnhancerDao;
import com.xbk.knowledge.infrastructure.dao.po.AgentEnhancerPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * AgentEnhancer 资产仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class AgentEnhancerRepositoryImpl implements AgentEnhancerRepository {

    private final IAgentEnhancerDao mapper;

    /**
     * 按主键查询 AgentEnhancer 配置。
     *
     * @param id AgentEnhancer 主键
     * @return AgentEnhancer 配置（不存在时返回空）
     */
    @Override
    public Optional<AgentEnhancer> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(id))
                .map(item -> BeanMappingUtils.map(item, AgentEnhancer.class));
    }

    /**
     * 按编码查询 AgentEnhancer 配置。
     *
     * @param agentEnhancerCode AgentEnhancer 业务编码
     * @return AgentEnhancer 配置（不存在时返回空）
     */
    @Override
    public Optional<AgentEnhancer> findByCode(String agentEnhancerCode) {
        if (!StringUtils.hasText(agentEnhancerCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByCode(agentEnhancerCode))
                .map(item -> BeanMappingUtils.map(item, AgentEnhancer.class));
    }

    /**
     * 分页查询 AgentEnhancer 列表。
     *
     * @param query 分页与筛选条件
     * @return AgentEnhancer 分页数据列表
     */
    @Override
    public List<AgentEnhancer> findPage(AgentEnhancerPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findPage(query), AgentEnhancer.class);
    }

    /**
     * 统计 AgentEnhancer 查询结果总数。
     *
     * @param query 分页与筛选条件
     * @return 命中总数
     */
    @Override
    public long count(AgentEnhancerPageQuery query) {
        if (query == null) {
            return 0;
        }
        return mapper.count(query);
    }

    /**
     * 新增 AgentEnhancer 配置。
     *
     * @param agentEnhancer AgentEnhancer 领域实体
     * @return 新增后的 AgentEnhancer 领域实体
     */
    @Override
    public AgentEnhancer insert(AgentEnhancer agentEnhancer) {
        if (agentEnhancer == null) {
            return null;
        }
        mapper.insertAgentEnhancer(BeanMappingUtils.map(agentEnhancer, AgentEnhancerPO.class));
        return agentEnhancer;
    }

    /**
     * 更新 AgentEnhancer 配置。
     *
     * @param agentEnhancer AgentEnhancer 领域实体
     * @return 更新影响行数
     */
    @Override
    public int update(AgentEnhancer agentEnhancer) {
        if (agentEnhancer == null || agentEnhancer.getId() == null) {
            return 0;
        }
        return mapper.updateAgentEnhancer(BeanMappingUtils.map(agentEnhancer, AgentEnhancerPO.class));
    }

    /**
     * 更新 AgentEnhancer 启用状态。
     *
     * @param id AgentEnhancer 主键
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
     * 按主键删除 AgentEnhancer 配置。
     *
     * @param id AgentEnhancer 主键
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
