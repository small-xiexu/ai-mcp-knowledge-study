package com.xbk.knowledge.infrastructure.mapper.prompt;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.agent.PromptTemplate;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplatePageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * PromptTemplate Mapper（通过 XML 承载 SQL）。
 
  * @author xiexu
  */
@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {

    int insertTemplate(PromptTemplate template);

    int updateDraft(PromptTemplate template);

    PromptTemplate findById(PromptTemplateIdQuery query);

    long count(PromptTemplatePageQuery query);

    List<PromptTemplate> findPage(PromptTemplatePageQuery query);

    int countByOrgIdAndCode(@Param("orgId") Long orgId, @Param("templateCode") String templateCode);

    int publish(@Param("orgId") Long orgId, @Param("id") Long id, @Param("updatedBy") Long updatedBy);

    int archive(@Param("orgId") Long orgId, @Param("id") Long id, @Param("updatedBy") Long updatedBy);
}

