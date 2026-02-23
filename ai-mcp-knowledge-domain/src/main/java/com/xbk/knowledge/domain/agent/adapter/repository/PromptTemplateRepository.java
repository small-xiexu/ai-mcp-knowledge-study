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
     * 按主键查询记录。
     */
    Optional<PromptTemplate> findById(PromptTemplateIdQuery query);

    /**
     * 判断编码是否已存在。
     */
    boolean existsByCode(String templateCode);

    /**
     * 新增记录。
     */
    PromptTemplate insert(PromptTemplate template);

    /**
     * 更新草稿版本内容。
     */
    int updateDraft(PromptTemplate template);

    /**
     * 发布版本并写入快照信息。
     */
    int publish(Long id, Long updatedBy);

    /**
     * 归档模板版本。
     */
    int archive(Long id, Long updatedBy);

    /**
     * 按条件分页查询记录。
     */
    List<PromptTemplate> findPage(PromptTemplatePageQuery query);

    /**
     * 统计符合条件的记录数量。
     */
    long count(PromptTemplatePageQuery query);
}
