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

    private final IPromptTemplateService promptTemplateService;

    /**
     * queryPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public PageResult<PromptTemplate> queryPage(PromptTemplatePageQuery query) {
        return promptTemplateService.queryPage(query);
    }

    /**
     * queryById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public PromptTemplate queryById(PromptTemplateIdQuery query) {
        return promptTemplateService.queryById(query);
    }

    /**
     * create。
     *
     * @param template 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate create(PromptTemplate template) {
        return promptTemplateService.create(template);
    }

    /**
     * updateDraft。
     *
     * @param template 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate updateDraft(PromptTemplate template) {
        return promptTemplateService.updateDraft(template);
    }

    /**
     * publish。
     *
     * @param query 参数
     * @param operatorId 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate publish(PromptTemplateIdQuery query, Long operatorId) {
        return promptTemplateService.publish(query, operatorId);
    }

    /**
     * archive。
     *
     * @param query 参数
     * @param operatorId 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate archive(PromptTemplateIdQuery query, Long operatorId) {
        return promptTemplateService.archive(query, operatorId);
    }
}
