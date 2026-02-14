package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.types.context.OrgContext;

/**
 * 组织上下文解析服务。
 *
 * 职责：从登录用户与请求携带的 targetOrgId 解析当前请求的组织视角，
 * 支持“超管跨组织管理需显式选择目标组织”的治理约束。
 */
public interface OrgContextService {

    /**
     * 解析当前请求的组织上下文。
     *
     * @param userId 登录用户ID
     * @param targetOrgIdText 请求携带的目标组织ID（可空）
     * @return OrgContext
     */
    OrgContext resolve(Long userId, String targetOrgIdText);
}

