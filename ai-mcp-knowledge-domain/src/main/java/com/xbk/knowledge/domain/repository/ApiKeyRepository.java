package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.SysApiKey;
import com.xbk.knowledge.domain.model.vo.identity.ApiKeyPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * API Key 仓储接口。
 *
 * 职责：服务账号密钥数据访问抽象。
 *
 * @author xiexu
 */
public interface ApiKeyRepository {

    /**
     * 分页查询 API Key。
     *
     * @param query 查询条件
     * @return 列表
     */
    List<SysApiKey> findPage(ApiKeyPageQuery query);

    /**
     * 统计 API Key 数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    long count(ApiKeyPageQuery query);

    /**
     * 新增 API Key。
     *
     * @param apiKey 实体
     * @return 新增后的实体
     */
    SysApiKey insert(SysApiKey apiKey);

    /**
     * 按ID查询 API Key。
     *
     * @param id ID
     * @return API Key
     */
    Optional<SysApiKey> findById(Long id);

    /**
     * 更新 API Key 状态。
     *
     * @param id ID
     * @param tenantId 租户ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(Long id, String tenantId, Integer status);
}
