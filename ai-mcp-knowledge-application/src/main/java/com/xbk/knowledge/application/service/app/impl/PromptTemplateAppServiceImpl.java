package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.PromptTemplateAppService;
import com.xbk.knowledge.domain.model.entity.agent.PromptTemplate;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplatePageQuery;
import com.xbk.knowledge.domain.service.prompt.IPromptTemplateService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PromptTemplate 控制面应用服务实现。
 
  * @author xiexu
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
        OrgContext ctx = OrgContextHolder.get();
        boolean superAdmin = ctx != null && ctx.superAdmin();
        boolean global = template != null && "GLOBAL".equalsIgnoreCase(template.getScope());
        if (global && !superAdmin) {
            throw new BusinessException("仅超级管理员可管理 GLOBAL 模板");
        }
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
        OrgContext ctx = OrgContextHolder.get();
        boolean superAdmin = ctx != null && ctx.superAdmin();
        PromptTemplate existed = promptTemplateService.queryById(new PromptTemplateIdQuery(template.getOrgId(), template.getId()));
        boolean global = "GLOBAL".equalsIgnoreCase(existed.getScope());
        if (global && !superAdmin) {
            throw new BusinessException("GLOBAL 模板仅允许超级管理员编辑");
        }
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
        OrgContext ctx = OrgContextHolder.get();
        boolean superAdmin = ctx != null && ctx.superAdmin();
        PromptTemplate existed = promptTemplateService.queryById(query);
        boolean global = "GLOBAL".equalsIgnoreCase(existed.getScope());
        if (global && !superAdmin) {
            throw new BusinessException("GLOBAL 模板仅允许超级管理员发布");
        }
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
        OrgContext ctx = OrgContextHolder.get();
        boolean superAdmin = ctx != null && ctx.superAdmin();
        PromptTemplate existed = promptTemplateService.queryById(query);
        boolean global = "GLOBAL".equalsIgnoreCase(existed.getScope());
        if (global && !superAdmin) {
            throw new BusinessException("GLOBAL 模板仅允许超级管理员归档");
        }
        return promptTemplateService.archive(query, operatorId);
    }
}
