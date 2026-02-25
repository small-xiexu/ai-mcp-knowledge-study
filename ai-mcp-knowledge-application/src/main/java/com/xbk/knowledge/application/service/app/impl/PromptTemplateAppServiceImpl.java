package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.PromptTemplateAppService;
import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;
import com.xbk.knowledge.domain.agent.service.IPromptTemplateService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PromptTemplate 控制面应用服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateAppServiceImpl implements PromptTemplateAppService {
    /**
     * PromptTemplate 领域服务，用于模板草稿与发布生命周期管理。
     */
    private final IPromptTemplateService promptTemplateService;

    /**
     * 查询提示词模板。
     *
     * @param query 分页查询条件
     * @return PromptTemplate 分页数据
     */
    @Override
    public PageResult<PromptTemplate> queryPage(PromptTemplatePageQuery query) {
        return promptTemplateService.queryPage(query);
    }

    /**
     * 查询提示词模板。
     *
     * @param query 主键查询条件
     * @return PromptTemplate 详情
     */
    @Override
    public PromptTemplate queryById(PromptTemplateIdQuery query) {
        return promptTemplateService.queryById(query);
    }

    /**
     * 创建并持久化提示词模板数据。
     *
     * @param template 模板实体
     * @return 创建后的 PromptTemplate 信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate create(PromptTemplate template) {
        return promptTemplateService.create(template);
    }

    /**
     * 更新提示词模板数据。
     *
     * @param template 模板实体
     * @return 更新后的 PromptTemplate 信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate updateDraft(PromptTemplate template) {
        return promptTemplateService.updateDraft(template);
    }

    /**
     * 发布业务配置。
     *
     * @param query 主键查询条件
     * @param operatorId 操作人 ID
     * @return 发布后的 PromptTemplate 信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate publish(PromptTemplateIdQuery query, Long operatorId) {
        return promptTemplateService.publish(query, operatorId);
    }

    /**
     * 归档业务配置。
     *
     * @param query 主键查询条件
     * @param operatorId 操作人 ID
     * @return 归档后的 PromptTemplate 信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate archive(PromptTemplateIdQuery query, Long operatorId) {
        return promptTemplateService.archive(query, operatorId);
    }
}
