package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.entity.agent.PromptTemplate;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplatePageQuery;
import com.xbk.knowledge.domain.repository.PromptTemplateRepository;
import com.xbk.knowledge.domain.service.IPromptTemplateService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PromptTemplate 领域服务实现。
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements IPromptTemplateService {

    private static final long GLOBAL_ORG_ID = 0L;

    private final PromptTemplateRepository promptTemplateRepository;

    @Override
    public PageResult<PromptTemplate> queryPage(PromptTemplatePageQuery query) {
        if (query == null || query.getOrgId() == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        PromptTemplatePageQuery normalized = new PromptTemplatePageQuery(
                query.getOrgId(),
                query.getKeyword(),
                query.getScope(),
                query.getState(),
                offset,
                pageSize
        );
        List<PromptTemplate> records = promptTemplateRepository.findPage(normalized);
        long total = promptTemplateRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    @Override
    public PromptTemplate queryById(PromptTemplateIdQuery query) {
        if (query == null || query.getOrgId() == null || query.getId() == null) {
            throw new IllegalArgumentException("orgId/id 不能为空");
        }
        return promptTemplateRepository
                .findById(query)
                .orElseThrow(() -> new NotFoundException("模板不存在，id: " + query.getId()));
    }

    @Override
    public PromptTemplate create(PromptTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("template 不能为空");
        }
        if (template.getScope() == null || template.getScope().isBlank()) {
            template.setScope("ORG");
        }
        if (template.getTemplateCode() == null || template.getTemplateCode().isBlank()) {
            throw new IllegalArgumentException("templateCode 不能为空");
        }
        if (template.getTemplateName() == null || template.getTemplateName().isBlank()) {
            throw new IllegalArgumentException("templateName 不能为空");
        }
        if (template.getContent() == null || template.getContent().isBlank()) {
            throw new IllegalArgumentException("content 不能为空");
        }

        if ("GLOBAL".equalsIgnoreCase(template.getScope())) {
            template.setScope("GLOBAL");
            template.setOrgId(GLOBAL_ORG_ID);
        } else {
            template.setScope("ORG");
            if (template.getOrgId() == null) {
                throw new IllegalArgumentException("orgId 不能为空");
            }
        }

        if (promptTemplateRepository.existsByOrgIdAndCode(template.getOrgId(), template.getTemplateCode())) {
            throw new BusinessException("templateCode 已存在：" + template.getTemplateCode());
        }

        LocalDateTime now = LocalDateTime.now();
        template.setState("DRAFT");
        // 将首次 publish 后的 versionNo 置为 1，因此 draft 初始化为 0
        template.setVersionNo(0);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        return promptTemplateRepository.insert(template);
    }

    @Override
    public PromptTemplate updateDraft(PromptTemplate template) {
        if (template == null || template.getId() == null || template.getOrgId() == null) {
            throw new IllegalArgumentException("id/orgId 不能为空");
        }
        PromptTemplate existed = queryById(new PromptTemplateIdQuery(template.getOrgId(), template.getId()));
        if (!"DRAFT".equalsIgnoreCase(existed.getState())) {
            throw new BusinessException("仅 DRAFT 模板允许编辑");
        }
        if ("GLOBAL".equalsIgnoreCase(existed.getScope())) {
            // orgId 必须是 0（由数据层约束），此处只保证语义清晰
            existed.setOrgId(GLOBAL_ORG_ID);
        }
        if (template.getTemplateName() != null) {
            existed.setTemplateName(template.getTemplateName());
        }
        if (template.getContent() != null) {
            existed.setContent(template.getContent());
        }
        if (template.getVariableSpecJson() != null) {
            existed.setVariableSpecJson(template.getVariableSpecJson());
        }
        existed.setUpdatedBy(template.getUpdatedBy());
        existed.setUpdatedAt(LocalDateTime.now());
        int affected = promptTemplateRepository.updateDraft(existed);
        if (affected <= 0) {
            throw new BusinessException("模板更新失败，id: " + template.getId());
        }
        return queryById(new PromptTemplateIdQuery(template.getOrgId(), template.getId()));
    }

    @Override
    public PromptTemplate publish(PromptTemplateIdQuery query, Long updatedBy) {
        PromptTemplate existed = queryById(query);
        if (!"DRAFT".equalsIgnoreCase(existed.getState())) {
            throw new BusinessException("仅 DRAFT 模板允许发布");
        }
        int affected = promptTemplateRepository.publish(existed.getOrgId(), existed.getId(), updatedBy);
        if (affected <= 0) {
            throw new BusinessException("模板发布失败，id: " + existed.getId());
        }
        return queryById(new PromptTemplateIdQuery(query.getOrgId(), query.getId()));
    }

    @Override
    public PromptTemplate archive(PromptTemplateIdQuery query, Long updatedBy) {
        PromptTemplate existed = queryById(query);
        int affected = promptTemplateRepository.archive(existed.getOrgId(), existed.getId(), updatedBy);
        if (affected <= 0) {
            throw new BusinessException("模板归档失败，id: " + existed.getId());
        }
        return queryById(new PromptTemplateIdQuery(query.getOrgId(), query.getId()));
    }
}

