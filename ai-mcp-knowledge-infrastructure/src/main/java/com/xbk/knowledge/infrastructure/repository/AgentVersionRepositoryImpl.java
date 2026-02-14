package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionPageQuery;
import com.xbk.knowledge.domain.repository.AgentVersionRepository;
import com.xbk.knowledge.infrastructure.mapper.AgentVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * AgentVersion 仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class AgentVersionRepositoryImpl implements AgentVersionRepository {

    private final AgentVersionMapper agentVersionMapper;

    @Override
    public Optional<AgentVersion> findById(AgentVersionIdQuery query) {
        if (query == null || query.getOrgId() == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentVersionMapper.findById(query));
    }

    @Override
    public List<AgentVersion> findPage(AgentVersionPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return agentVersionMapper.findPage(query);
    }

    @Override
    public long count(AgentVersionPageQuery query) {
        if (query == null) {
            return 0;
        }
        return agentVersionMapper.count(query);
    }

    @Override
    public Integer findMaxVersionNo(Long agentId) {
        if (agentId == null) {
            return null;
        }
        return agentVersionMapper.findMaxVersionNo(agentId);
    }

    @Override
    public boolean existsByAgentIdAndVersionNo(Long agentId, Integer versionNo) {
        if (agentId == null || versionNo == null) {
            return false;
        }
        return agentVersionMapper.countByAgentIdAndVersionNo(agentId, versionNo) > 0;
    }

    @Override
    public AgentVersion insert(AgentVersion version) {
        if (version == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (version.getCreatedAt() == null) {
            version.setCreatedAt(now);
        }
        if (version.getUpdatedAt() == null) {
            version.setUpdatedAt(now);
        }
        agentVersionMapper.insertAgentVersion(version);
        return version;
    }

    @Override
    public int updateDraft(AgentVersion version) {
        if (version == null || version.getId() == null || version.getOrgId() == null) {
            return 0;
        }
        if (version.getUpdatedAt() == null) {
            version.setUpdatedAt(LocalDateTime.now());
        }
        return agentVersionMapper.updateDraft(version);
    }

    @Override
    public int publish(Long orgId,
                       Long id,
                       Integer promptTemplateVersionNo,
                       String templateParamsJson,
                       String systemPromptSnapshot,
                       Long updatedBy) {
        if (orgId == null || id == null) {
            return 0;
        }
        return agentVersionMapper.publish(
                orgId,
                id,
                promptTemplateVersionNo,
                templateParamsJson,
                systemPromptSnapshot,
                updatedBy
        );
    }

    @Override
    public int updateState(Long orgId, Long id, String fromState, String toState) {
        if (orgId == null || id == null) {
            return 0;
        }
        return agentVersionMapper.updateState(orgId, id, fromState, toState);
    }
}
