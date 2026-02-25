package com.xbk.knowledge.domain.agent.service.impl;

import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.PromptTemplateRepository;
import com.xbk.knowledge.domain.agent.service.IPromptTemplateService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PromptTemplate 领域服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements IPromptTemplateService {
    /**
     * PromptTemplate 仓储，用于模板草稿/发布态持久化操作。
     */
    private final PromptTemplateRepository promptTemplateRepository;

    /**
     * 查询提示词模板。
     *
     * @param query 分页查询条件
     * @return PromptTemplate 分页数据
     */
    @Override
    public PageResult<PromptTemplate> queryPage(PromptTemplatePageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query 不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        PromptTemplatePageQuery normalized = new PromptTemplatePageQuery(
                query.getKeyword(),
                query.getState(),
                offset,
                pageSize
        );
        List<PromptTemplate> records = promptTemplateRepository.findPage(normalized);
        long total = promptTemplateRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 查询提示词模板。
     *
     * @param query 主键查询条件
     * @return PromptTemplate 详情
     */
    @Override
    public PromptTemplate queryById(PromptTemplateIdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        return promptTemplateRepository
                .findById(query)
                .orElseThrow(() -> new NotFoundException("模板不存在，id: " + query.getId()));
    }

    /**
     * 创建并持久化提示词模板数据。
     *
     * @param template 模板实体
     * @return 创建后的 PromptTemplate 信息
     */
    @Override
    public PromptTemplate create(PromptTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("template 不能为空");
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

        if (promptTemplateRepository.existsByCode(template.getTemplateCode())) {
            throw new BusinessException("templateCode 已存在" + template.getTemplateCode());
        }

        LocalDateTime now = LocalDateTime.now();
        template.setState("DRAFT");
        // 将首次 publish 后的 versionNo 置为 1，因此 draft 初始化为 0
        template.setVersionNo(0);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        return promptTemplateRepository.insert(template);
    }

    /**
     * 更新提示词模板数据。
     *
     * @param template 模板实体
     * @return 更新后的 PromptTemplate 信息
     */
    @Override
    public PromptTemplate updateDraft(PromptTemplate template) {
        if (template == null || template.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        PromptTemplate existed = queryById(new PromptTemplateIdQuery(template.getId()));
        if (!"DRAFT".equalsIgnoreCase(existed.getState())) {
            throw new BusinessException("仅 DRAFT 模板允许编辑");
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
        return queryById(new PromptTemplateIdQuery(template.getId()));
    }

    /**
     * 发布业务配置。
     *
     * @param query 主键查询条件
     * @param updatedBy 更新人 ID
     * @return 发布后的 PromptTemplate 信息
     */
    @Override
    public PromptTemplate publish(PromptTemplateIdQuery query, Long updatedBy) {
        PromptTemplate existed = queryById(query);
        if (!"DRAFT".equalsIgnoreCase(existed.getState())) {
            throw new BusinessException("仅 DRAFT 模板允许发布");
        }
        int affected = promptTemplateRepository.publish(existed.getId(), updatedBy);
        if (affected <= 0) {
            throw new BusinessException("模板发布失败，id: " + existed.getId());
        }
        return queryById(new PromptTemplateIdQuery(query.getId()));
    }

    /**
     * 归档业务配置。
     *
     * @param query 主键查询条件
     * @param updatedBy 更新人 ID
     * @return 归档后的 PromptTemplate 信息
     */
    @Override
    public PromptTemplate archive(PromptTemplateIdQuery query, Long updatedBy) {
        PromptTemplate existed = queryById(query);
        int affected = promptTemplateRepository.archive(existed.getId(), updatedBy);
        if (affected <= 0) {
            throw new BusinessException("模板归档失败，id: " + existed.getId());
        }
        return queryById(new PromptTemplateIdQuery(query.getId()));
    }
}
