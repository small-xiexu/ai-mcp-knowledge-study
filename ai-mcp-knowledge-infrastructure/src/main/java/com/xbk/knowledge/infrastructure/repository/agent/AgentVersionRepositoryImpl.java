package com.xbk.knowledge.infrastructure.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionPageQuery;
import com.xbk.knowledge.domain.repository.agent.AgentVersionRepository;
import com.xbk.knowledge.infrastructure.mapper.agent.AgentVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * AgentVersion 仓储实现。
 
  * @author xiexu
  */
@Repository
@RequiredArgsConstructor
public class AgentVersionRepositoryImpl implements AgentVersionRepository {

    private final AgentVersionMapper agentVersionMapper;

    /**
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentVersion> findById(AgentVersionIdQuery query) {
        if (query == null || query.getOrgId() == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentVersionMapper.findById(query));
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<AgentVersion> findPage(AgentVersionPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return agentVersionMapper.findPage(query);
    }

    /**
     * count。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long count(AgentVersionPageQuery query) {
        if (query == null) {
            return 0;
        }
        return agentVersionMapper.count(query);
    }

    /**
     * findMaxVersionNo。
     *
     * @param agentId 参数
     * @return 返回结果
     */
    @Override
    public Integer findMaxVersionNo(Long agentId) {
        if (agentId == null) {
            return null;
        }
        return agentVersionMapper.findMaxVersionNo(agentId);
    }

    /**
     * existsByAgentIdAndVersionNo。
     *
     * @param agentId 参数
     * @param versionNo 参数
     * @return 返回结果
     */
    @Override
    public boolean existsByAgentIdAndVersionNo(Long agentId, Integer versionNo) {
        if (agentId == null || versionNo == null) {
            return false;
        }
        return agentVersionMapper.countByAgentIdAndVersionNo(agentId, versionNo) > 0;
    }

    /**
     * insert。
     *
     * @param version 参数
     * @return 返回结果
     */
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

    /**
     * updateDraft。
     *
     * @param version 参数
     * @return 返回结果
     */
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

    /**
     * updateState。
     *
     * @param orgId 参数
     * @param id 参数
     * @param fromState 参数
     * @param toState 参数
     * @return 返回结果
     */
    @Override
    public int updateState(Long orgId, Long id, String fromState, String toState) {
        if (orgId == null || id == null) {
            return 0;
        }
        return agentVersionMapper.updateState(orgId, id, fromState, toState);
    }
}
