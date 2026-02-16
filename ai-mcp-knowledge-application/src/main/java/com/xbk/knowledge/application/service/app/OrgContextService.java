package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.types.context.OrgContext;

/**
 * 组织上下文解析服务。
 *
 * 职责：从登录用户解析当前请求的组织视角。
 
  * @author xiexu
  */
public interface OrgContextService {

    /**
     * 解析当前请求的组织上下文。
     *
     * @param userId 登录用户ID
     * @return OrgContext
     */
    OrgContext resolve(Long userId);
}
