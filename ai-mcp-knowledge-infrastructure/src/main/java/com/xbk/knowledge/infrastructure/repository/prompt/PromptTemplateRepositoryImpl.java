package com.xbk.knowledge.infrastructure.repository.prompt;

import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.PromptTemplateRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IPromptTemplateDao;
import com.xbk.knowledge.infrastructure.dao.po.PromptTemplatePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * PromptTemplate 仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class PromptTemplateRepositoryImpl implements PromptTemplateRepository {

    private final IPromptTemplateDao promptTemplateMapper;

    /**
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<PromptTemplate> findById(PromptTemplateIdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(promptTemplateMapper.findById(query))
                .map(item -> BeanMappingUtils.map(item, PromptTemplate.class));
    }

    /**
     * existsByCode。
     *
     * @param templateCode 参数
     * @return 返回结果
     */
    @Override
    public boolean existsByCode(String templateCode) {
        if (templateCode == null) {
            return false;
        }
        return promptTemplateMapper.countByCode(templateCode) > 0;
    }

    /**
     * insert。
     *
     * @param template 参数
     * @return 返回结果
     */
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
        promptTemplateMapper.insertTemplate(BeanMappingUtils.map(template, PromptTemplatePO.class));
        return template;
    }

    /**
     * updateDraft。
     *
     * @param template 参数
     * @return 返回结果
     */
    @Override
    public int updateDraft(PromptTemplate template) {
        if (template == null || template.getId() == null) {
            return 0;
        }
        if (template.getUpdatedAt() == null) {
            template.setUpdatedAt(LocalDateTime.now());
        }
        return promptTemplateMapper.updateDraft(BeanMappingUtils.map(template, PromptTemplatePO.class));
    }

    /**
     * publish。
     *
     * @param id 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    public int publish(Long id, Long updatedBy) {
        if (id == null) {
            return 0;
        }
        return promptTemplateMapper.publish(id, updatedBy);
    }

    /**
     * archive。
     *
     * @param id 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    public int archive(Long id, Long updatedBy) {
        if (id == null) {
            return 0;
        }
        return promptTemplateMapper.archive(id, updatedBy);
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<PromptTemplate> findPage(PromptTemplatePageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(promptTemplateMapper.findPage(query), PromptTemplate.class);
    }

    /**
     * count。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long count(PromptTemplatePageQuery query) {
        if (query == null) {
            return 0;
        }
        return promptTemplateMapper.count(query);
    }
}
