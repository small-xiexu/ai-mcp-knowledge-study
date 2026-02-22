package com.xbk.knowledge.infrastructure.agent.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionPageQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentVersionRepository;
import com.xbk.knowledge.infrastructure.dao.IAgentVersionDao;
import com.xbk.knowledge.infrastructure.dao.po.AgentVersionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AgentVersion 仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class AgentVersionRepositoryImpl implements AgentVersionRepository {

    private final IAgentVersionDao agentVersionDao;

    /**
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentVersion> findById(AgentVersionIdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toEntity(agentVersionDao.findById(query)));
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
        return agentVersionDao.findPage(query)
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * listByAgentId。
     *
     * @param agentId 参数
     * @return 返回结果
     */
    @Override
    public List<AgentVersion> listByAgentId(Long agentId) {
        if (agentId == null) {
            return Collections.emptyList();
        }
        return agentVersionDao.listByAgentId(agentId)
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
    public long count(AgentVersionPageQuery query) {
        if (query == null) {
            return 0;
        }
        return agentVersionDao.count(query);
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
        return agentVersionDao.findMaxVersionNo(agentId);
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
        return agentVersionDao.countByAgentIdAndVersionNo(agentId, versionNo) > 0;
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
        AgentVersionPO po = toPO(version);
        agentVersionDao.insertAgentVersion(po);
        version.setId(po.getId());
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
        if (version == null || version.getId() == null) {
            return 0;
        }
        if (version.getUpdatedAt() == null) {
            version.setUpdatedAt(LocalDateTime.now());
        }
        return agentVersionDao.updateDraft(toPO(version));
    }

    /**
     * publish。
     *
     * @param id 参数
     * @param promptTemplateVersionNo 参数
     * @param templateParamsJson 参数
     * @param systemPromptSnapshot 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    public int publish(Long id,
                       Integer promptTemplateVersionNo,
                       String templateParamsJson,
                       String systemPromptSnapshot,
                       Long updatedBy) {
        if (id == null) {
            return 0;
        }
        return agentVersionDao.publish(id,
                promptTemplateVersionNo,
                templateParamsJson,
                systemPromptSnapshot,
                updatedBy
        );
    }

    /**
     * updateState。
     *
     * @param id 参数
     * @param fromState 参数
     * @param toState 参数
     * @return 返回结果
     */
    @Override
    public int updateState(Long id, String fromState, String toState) {
        if (id == null) {
            return 0;
        }
        return agentVersionDao.updateState(id, fromState, toState);
    }

    /**
     * removeByAgentId。
     *
     * @param agentId 参数
     * @return 返回结果
     */
    @Override
    public int removeByAgentId(Long agentId) {
        if (agentId == null) {
            return 0;
        }
        return agentVersionDao.deleteByAgentId(agentId);
    }

    /**
     * 实体转持久化对象。
     */
    private AgentVersionPO toPO(AgentVersion version) {
        if (version == null) {
            return null;
        }
        return AgentVersionPO.builder()
                .id(version.getId())
                .agentId(version.getAgentId())
                .versionNo(version.getVersionNo())
                .state(version.getState())
                .changeSummary(version.getChangeSummary())
                .promptTemplateId(version.getPromptTemplateId())
                .promptTemplateVersionNo(version.getPromptTemplateVersionNo())
                .templateParamsJson(version.getTemplateParamsJson())
                .systemPromptSnapshot(version.getSystemPromptSnapshot())
                .workflowVersionId(version.getWorkflowVersionId())
                .outputContractVersion(version.getOutputContractVersion())
                .outputContractOptionsJson(version.getOutputContractOptionsJson())
                .ragMode(version.getRagMode())
                .defaultRagTagsJson(version.getDefaultRagTagsJson())
                .allowedRagTagsJson(version.getAllowedRagTagsJson())
                .allowedToolKeysJson(version.getAllowedToolKeysJson())
                .clientProfileId(version.getClientProfileId())
                .clientChainJson(version.getClientChainJson())
                .timeoutMs(version.getTimeoutMs())
                .maxTurns(version.getMaxTurns())
                .temperature(version.getTemperature())
                .repairRetryTimes(version.getRepairRetryTimes())
                .createdBy(version.getCreatedBy())
                .updatedBy(version.getUpdatedBy())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .build();
    }

    /**
     * 持久化对象转实体。
     */
    private AgentVersion toEntity(AgentVersionPO po) {
        if (po == null) {
            return null;
        }
        return AgentVersion.builder()
                .id(po.getId())
                .agentId(po.getAgentId())
                .versionNo(po.getVersionNo())
                .state(po.getState())
                .changeSummary(po.getChangeSummary())
                .promptTemplateId(po.getPromptTemplateId())
                .promptTemplateVersionNo(po.getPromptTemplateVersionNo())
                .templateParamsJson(po.getTemplateParamsJson())
                .systemPromptSnapshot(po.getSystemPromptSnapshot())
                .workflowVersionId(po.getWorkflowVersionId())
                .outputContractVersion(po.getOutputContractVersion())
                .outputContractOptionsJson(po.getOutputContractOptionsJson())
                .ragMode(po.getRagMode())
                .defaultRagTagsJson(po.getDefaultRagTagsJson())
                .allowedRagTagsJson(po.getAllowedRagTagsJson())
                .allowedToolKeysJson(po.getAllowedToolKeysJson())
                .clientProfileId(po.getClientProfileId())
                .clientChainJson(po.getClientChainJson())
                .timeoutMs(po.getTimeoutMs())
                .maxTurns(po.getMaxTurns())
                .temperature(po.getTemperature())
                .repairRetryTimes(po.getRepairRetryTimes())
                .createdBy(po.getCreatedBy())
                .updatedBy(po.getUpdatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
