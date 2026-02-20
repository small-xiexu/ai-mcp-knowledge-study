package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * PromptTemplate 控制面应用服务。
 
  * @author xiexu
  */
public interface PromptTemplateAppService {

    PageResult<PromptTemplate> queryPage(PromptTemplatePageQuery query);

    PromptTemplate queryById(PromptTemplateIdQuery query);

    PromptTemplate create(PromptTemplate template);

    PromptTemplate updateDraft(PromptTemplate template);

    PromptTemplate publish(PromptTemplateIdQuery query, Long operatorId);

    PromptTemplate archive(PromptTemplateIdQuery query, Long operatorId);
}

