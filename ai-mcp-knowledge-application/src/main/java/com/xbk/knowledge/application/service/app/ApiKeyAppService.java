package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.application.model.identity.ApiKeyCreateResult;
import com.xbk.knowledge.domain.model.entity.SysApiKey;
import com.xbk.knowledge.domain.model.vo.identity.ApiKeyPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 管理应用服务接口。
 *
 * 职责：应用层用例接口，用于封装服务账号密钥管理能力。
 *
 * @author xiexu
 */
public interface ApiKeyAppService {

    /**
     * 分页查询 API Key。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SysApiKey> queryPage(ApiKeyPageQuery query);

    /**
     * 创建 API Key。
     *
     * @param tenantId 租户ID
     * @param ownerUserId 归属用户ID
     * @param scopes 权限范围
     * @param expireAt 过期时间
     * @return 创建结果
     */
    ApiKeyCreateResult create(String tenantId, Long ownerUserId, List<String> scopes, LocalDateTime expireAt);

    /**
     * 禁用 API Key。
     *
     * @param tenantId 租户ID
     * @param id API Key ID
     */
    void revoke(String tenantId, Long id);
}
