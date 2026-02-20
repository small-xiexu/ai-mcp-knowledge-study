package com.xbk.knowledge.infrastructure.agent.repository;

import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRepository;
import com.xbk.knowledge.infrastructure.dao.IAgentDao;
import com.xbk.knowledge.infrastructure.dao.po.AgentPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Agent 仓储实现。
 
  * @author xiexu
  */
@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {

    private final IAgentDao agentDao;

    /**
     * findByCode。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<Agent> findByCode(AgentCodeQuery query) {
        if (query == null || query.getAgentCode() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toEntity(agentDao.findByCode(query)));
    }

    /**
     * existsByCode。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public boolean existsByCode(AgentCodeQuery query) {
        return findByCode(query).isPresent();
    }

    /**
     * insert。
     *
     * @param agent 参数
     * @return 返回结果
     */
    @Override
    public Agent insert(Agent agent) {
        if (agent == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (agent.getCreatedAt() == null) {
            agent.setCreatedAt(now);
        }
        if (agent.getUpdatedAt() == null) {
            agent.setUpdatedAt(now);
        }
        AgentPO po = toPO(agent);
        agentDao.insertAgent(po);
        agent.setId(po.getId());
        return agent;
    }

    /**
     * updateByCode。
     *
     * @param agent 参数
     * @return 返回结果
     */
    @Override
    public int updateByCode(Agent agent) {
        if (agent == null || agent.getAgentCode() == null) {
            return 0;
        }
        if (agent.getUpdatedAt() == null) {
            agent.setUpdatedAt(LocalDateTime.now());
        }
        return agentDao.updateAgentByCode(toPO(agent));
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<Agent> findPage(AgentPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return agentDao.findPage(query)
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * count。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long count(AgentPageQuery query) {
        if (query == null) {
            return 0;
        }
        return agentDao.count(query);
    }

    /**
     * countPublished。
     *
     * @return 返回结果
     */
    @Override
    public long countPublished() {
        return agentDao.countPublished();
    }

    /**
     * 实体转持久化对象。
     */
    private AgentPO toPO(Agent agent) {
        if (agent == null) {
            return null;
        }
        return AgentPO.builder()
                .id(agent.getId())
                .agentCode(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .status(agent.getStatus())
                .currentPublishedVersionId(agent.getCurrentPublishedVersionId())
                .createdBy(agent.getCreatedBy())
                .updatedBy(agent.getUpdatedBy())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

    /**
     * 持久化对象转实体。
     */
    private Agent toEntity(AgentPO po) {
        if (po == null) {
            return null;
        }
        return Agent.builder()
                .id(po.getId())
                .agentCode(po.getAgentCode())
                .agentName(po.getAgentName())
                .description(po.getDescription())
                .status(po.getStatus())
                .currentPublishedVersionId(po.getCurrentPublishedVersionId())
                .createdBy(po.getCreatedBy())
                .updatedBy(po.getUpdatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
