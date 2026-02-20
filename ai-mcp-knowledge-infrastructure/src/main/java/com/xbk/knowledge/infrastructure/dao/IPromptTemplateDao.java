package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.PromptTemplatePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * PromptTemplate Mapper（通过 XML 承载 SQL）。
 
  * @author xiexu
  */
@Mapper
public interface IPromptTemplateDao extends BaseMapper<PromptTemplatePO> {

    int insertTemplate(PromptTemplate template);

    int updateDraft(PromptTemplate template);

    PromptTemplate findById(PromptTemplateIdQuery query);

    long count(PromptTemplatePageQuery query);

    List<PromptTemplate> findPage(PromptTemplatePageQuery query);

    int countByCode(@Param("templateCode") String templateCode);

    int publish(@Param("id") Long id, @Param("updatedBy") Long updatedBy);

    int archive(@Param("id") Long id, @Param("updatedBy") Long updatedBy);
}
