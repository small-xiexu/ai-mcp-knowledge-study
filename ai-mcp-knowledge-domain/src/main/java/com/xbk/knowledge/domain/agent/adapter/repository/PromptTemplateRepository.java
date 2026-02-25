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
     * 
     * @param query 主键查询条件。
     * @return 可选的提示词模板实体。
     */
    Optional<PromptTemplate> findById(PromptTemplateIdQuery query);

    /**
     * 判断编码是否已存在。
     * 
     * @param templateCode 模板编码。
     * @return `true` 表示模板编码已存在，`false` 表示不存在。
     */
    boolean existsByCode(String templateCode);

    /**
     * 新增记录。
     * 
     * @param template 待新增的提示词模板实体。
     * @return 已持久化的提示词模板实体。
     */
    PromptTemplate insert(PromptTemplate template);

    /**
     * 更新草稿版本内容。
     * 
     * @param template 待更新的提示词模板实体。
     * @return 影响行数。
     */
    int updateDraft(PromptTemplate template);

    /**
     * 发布版本并写入快照信息。
     * 
     * @param id 主键 ID。
     * @param updatedBy 操作人标识。
     * @return 影响行数。
     */
    int publish(Long id, Long updatedBy);

    /**
     * 归档模板版本。
     * 
     * @param id 主键 ID。
     * @param updatedBy 操作人标识。
     * @return 影响行数。
     */
    int archive(Long id, Long updatedBy);

    /**
     * 按条件分页查询记录。
     * 
     * @param query 分页查询条件。
     * @return 提示词模板分页列表。
     */
    List<PromptTemplate> findPage(PromptTemplatePageQuery query);

    /**
     * 统计符合条件的记录数量。
     * 
     * @param query 分页查询条件。
     * @return 统计数量。
     */
    long count(PromptTemplatePageQuery query);
}
