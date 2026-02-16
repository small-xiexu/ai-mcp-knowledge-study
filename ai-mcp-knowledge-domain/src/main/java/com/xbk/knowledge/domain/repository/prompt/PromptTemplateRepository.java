package com.xbk.knowledge.domain.repository.prompt;

import com.xbk.knowledge.domain.model.entity.agent.PromptTemplate;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplatePageQuery;

import java.util.List;
import java.util.Optional;

/**
 * PromptTemplate 仓储接口。
 
  * @author xiexu
  */
public interface PromptTemplateRepository {

    Optional<PromptTemplate> findById(PromptTemplateIdQuery query);

    boolean existsByOrgIdAndCode(Long orgId, String templateCode);

    PromptTemplate insert(PromptTemplate template);

    int updateDraft(PromptTemplate template);

    int publish(Long orgId, Long id, Long updatedBy);

    int archive(Long orgId, Long id, Long updatedBy);

    List<PromptTemplate> findPage(PromptTemplatePageQuery query);

    long count(PromptTemplatePageQuery query);
}

