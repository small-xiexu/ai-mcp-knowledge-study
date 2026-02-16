package com.xbk.knowledge.infrastructure.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import com.xbk.knowledge.domain.repository.agent.AgentRepository;
import com.xbk.knowledge.infrastructure.mapper.agent.AgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Agent 仓储实现。
 
  * @author xiexu
  */
@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {

    private final AgentMapper agentMapper;

    /**
     * findByCode。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<Agent> findByCode(AgentCodeQuery query) {
        if (query == null || query.getOrgId() == null || query.getAgentCode() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentMapper.findByCode(query));
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
        agentMapper.insertAgent(agent);
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
        if (agent == null || agent.getOrgId() == null || agent.getAgentCode() == null) {
            return 0;
        }
        if (agent.getUpdatedAt() == null) {
            agent.setUpdatedAt(LocalDateTime.now());
        }
        return agentMapper.updateAgentByCode(agent);
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
        return agentMapper.findPage(query);
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
        return agentMapper.count(query);
    }

    /**
     * countPublishedByOrgId。
     *
     * @param orgId 参数
     * @return 返回结果
     */
    @Override
    public long countPublishedByOrgId(Long orgId) {
        if (orgId == null) {
            return 0;
        }
        return agentMapper.countPublishedByOrgId(orgId);
    }
}
