package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.agent.PromptTemplate;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplatePageQuery;
import com.xbk.knowledge.domain.repository.PromptTemplateRepository;
import com.xbk.knowledge.infrastructure.mapper.PromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * PromptTemplate 仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class PromptTemplateRepositoryImpl implements PromptTemplateRepository {

    private final PromptTemplateMapper promptTemplateMapper;

    @Override
    public Optional<PromptTemplate> findById(PromptTemplateIdQuery query) {
        if (query == null || query.getOrgId() == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(promptTemplateMapper.findById(query));
    }

    @Override
    public boolean existsByOrgIdAndCode(Long orgId, String templateCode) {
        if (orgId == null || templateCode == null) {
            return false;
        }
        return promptTemplateMapper.countByOrgIdAndCode(orgId, templateCode) > 0;
    }

    @Override
    public PromptTemplate insert(PromptTemplate template) {
        if (template == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getCreatedAt() == null) {
            template.setCreatedAt(now);
        }
        if (template.getUpdatedAt() == null) {
            template.setUpdatedAt(now);
        }
        promptTemplateMapper.insertTemplate(template);
        return template;
    }

    @Override
    public int updateDraft(PromptTemplate template) {
        if (template == null || template.getId() == null || template.getOrgId() == null) {
            return 0;
        }
        if (template.getUpdatedAt() == null) {
            template.setUpdatedAt(LocalDateTime.now());
        }
        return promptTemplateMapper.updateDraft(template);
    }

    @Override
    public int publish(Long orgId, Long id, Long updatedBy) {
        if (orgId == null || id == null) {
            return 0;
        }
        return promptTemplateMapper.publish(orgId, id, updatedBy);
    }

    @Override
    public int archive(Long orgId, Long id, Long updatedBy) {
        if (orgId == null || id == null) {
            return 0;
        }
        return promptTemplateMapper.archive(orgId, id, updatedBy);
    }

    @Override
    public List<PromptTemplate> findPage(PromptTemplatePageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return promptTemplateMapper.findPage(query);
    }

    @Override
    public long count(PromptTemplatePageQuery query) {
        if (query == null) {
            return 0;
        }
        return promptTemplateMapper.count(query);
    }
}

