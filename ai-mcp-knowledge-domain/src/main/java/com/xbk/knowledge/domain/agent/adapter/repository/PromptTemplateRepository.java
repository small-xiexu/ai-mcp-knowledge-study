package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;

import java.util.List;
import java.util.Optional;

/**
 * PromptTemplate 仓储接口。
 *
 * @author sxie
 */
public interface PromptTemplateRepository {

    /**
     * 方法：findById。
     */
    Optional<PromptTemplate> findById(PromptTemplateIdQuery query);

    /**
     * 方法：existsByCode。
     */
    boolean existsByCode(String templateCode);

    /**
     * 方法：insert。
     */
    PromptTemplate insert(PromptTemplate template);

    /**
     * 方法：updateDraft。
     */
    int updateDraft(PromptTemplate template);

    /**
     * 方法：publish。
     */
    int publish(Long id, Long updatedBy);

    /**
     * 方法：archive。
     */
    int archive(Long id, Long updatedBy);

    /**
     * 方法：findPage。
     */
    List<PromptTemplate> findPage(PromptTemplatePageQuery query);

    /**
     * 方法：count。
     */
    long count(PromptTemplatePageQuery query);
}
