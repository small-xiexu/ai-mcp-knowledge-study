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
     * 查询提示词模板。
     *
     * @param query 查询条件
     * @return 返回 PromptTemplate 查询结果（可能为空）。
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
     * 判断模板编码是否已存在。
     *
     * @param templateCode 模板编码。
     * @return 返回是否存在。
     */
    @Override
    public boolean existsByCode(String templateCode) {
        if (templateCode == null) {
            return false;
        }
        return promptTemplateMapper.countByCode(templateCode) > 0;
    }

    /**
     * 创建并持久化提示词模板数据。
     *
     * @param template 模板实体。
     * @return 返回 PromptTemplate 数据。
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
     * 更新提示词模板数据。
     *
     * @param template 模板实体。
     * @return 返回模板草稿更新条数。
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
     * 发布业务配置。
     *
     * @param id 主键 ID
     * @param updatedBy 更新人 ID
     * @return 返回模板发布更新条数。
     */
    @Override
    public int publish(Long id, Long updatedBy) {
        if (id == null) {
            return 0;
        }
        return promptTemplateMapper.publish(id, updatedBy);
    }

    /**
     * 归档业务配置。
     *
     * @param id 主键 ID
     * @param updatedBy 更新人 ID
     * @return 返回模板归档更新条数。
     */
    @Override
    public int archive(Long id, Long updatedBy) {
        if (id == null) {
            return 0;
        }
        return promptTemplateMapper.archive(id, updatedBy);
    }

    /**
     * 查询提示词模板。
     *
     * @param query 查询条件
     * @return 返回 PromptTemplate 列表数据。
     */
    @Override
    public List<PromptTemplate> findPage(PromptTemplatePageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(promptTemplateMapper.findPage(query), PromptTemplate.class);
    }

    /**
     * 按条件统计业务数量。
     *
     * @param query 查询条件
     * @return 统计数量
     */
    @Override
    public long count(PromptTemplatePageQuery query) {
        if (query == null) {
            return 0;
        }
        return promptTemplateMapper.count(query);
    }
}
