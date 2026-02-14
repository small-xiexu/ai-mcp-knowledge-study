package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import com.xbk.knowledge.domain.repository.AgentRepository;
import com.xbk.knowledge.infrastructure.mapper.AgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Agent 仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {

    private final AgentMapper agentMapper;

    @Override
    public Optional<Agent> findByCode(AgentCodeQuery query) {
        if (query == null || query.getOrgId() == null || query.getAgentCode() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentMapper.findByCode(query));
    }

    @Override
    public boolean existsByCode(AgentCodeQuery query) {
        return findByCode(query).isPresent();
    }

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
        agentMapper.insertAgent(agent);
        return agent;
    }

    @Override
    public int updateByCode(Agent agent) {
        if (agent == null || agent.getOrgId() == null || agent.getAgentCode() == null) {
            return 0;
        }
        if (agent.getUpdatedAt() == null) {
            agent.setUpdatedAt(LocalDateTime.now());
        }
        return agentMapper.updateAgentByCode(agent);
    }

    @Override
    public List<Agent> findPage(AgentPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return agentMapper.findPage(query);
    }

    @Override
    public long count(AgentPageQuery query) {
        if (query == null) {
            return 0;
        }
        return agentMapper.count(query);
    }
}

