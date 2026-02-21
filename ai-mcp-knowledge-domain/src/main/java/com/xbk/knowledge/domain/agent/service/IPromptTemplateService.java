package com.xbk.knowledge.domain.agent.service;

import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * PromptTemplate 领域服务接口。
 *
 * @author sxie
 */
public interface IPromptTemplateService {

    PageResult<PromptTemplate> queryPage(PromptTemplatePageQuery query);

    PromptTemplate queryById(PromptTemplateIdQuery query);

    PromptTemplate create(PromptTemplate template);

    PromptTemplate updateDraft(PromptTemplate template);

    PromptTemplate publish(PromptTemplateIdQuery query, Long updatedBy);

    PromptTemplate archive(PromptTemplateIdQuery query, Long updatedBy);
}

