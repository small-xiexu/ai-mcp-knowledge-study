package com.xbk.knowledge.domain.service.prompt;

import com.xbk.knowledge.domain.model.entity.agent.PromptTemplate;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplatePageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * PromptTemplate 领域服务接口。
 
  * @author xiexu
  */
public interface IPromptTemplateService {

    PageResult<PromptTemplate> queryPage(PromptTemplatePageQuery query);

    PromptTemplate queryById(PromptTemplateIdQuery query);

    PromptTemplate create(PromptTemplate template);

    PromptTemplate updateDraft(PromptTemplate template);

    PromptTemplate publish(PromptTemplateIdQuery query, Long updatedBy);

    PromptTemplate archive(PromptTemplateIdQuery query, Long updatedBy);
}

