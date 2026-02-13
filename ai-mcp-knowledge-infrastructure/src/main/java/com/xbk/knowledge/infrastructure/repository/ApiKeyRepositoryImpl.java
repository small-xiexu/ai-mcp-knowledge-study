package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.SysApiKey;
import com.xbk.knowledge.domain.model.vo.identity.ApiKeyPageQuery;
import com.xbk.knowledge.domain.repository.ApiKeyRepository;
import com.xbk.knowledge.infrastructure.mapper.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * API Key 仓储实现。
 *
 * 职责：基础设施层实现，用于落地 API Key 数据访问。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    private final ApiKeyMapper apiKeyMapper;

    /**
     * 分页查询 API Key。
     *
     * @param query 查询条件
     * @return 列表
     */
    @Override
    public List<SysApiKey> findPage(ApiKeyPageQuery query) {
        return apiKeyMapper.findPage(query);
    }

    /**
     * 统计 API Key 数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    @Override
    public long count(ApiKeyPageQuery query) {
        return apiKeyMapper.count(query);
    }

    /**
     * 新增 API Key。
     *
     * @param apiKey 实体
     * @return 新增后的实体
     */
    @Override
    public SysApiKey insert(SysApiKey apiKey) {
        apiKeyMapper.insertApiKey(apiKey);
        return apiKey;
    }

    /**
     * 按ID查询 API Key。
     *
     * @param id ID
     * @return API Key
     */
    @Override
    public Optional<SysApiKey> findById(Long id) {
        return Optional.ofNullable(apiKeyMapper.findById(id));
    }

    /**
     * 更新 API Key 状态。
     *
     * @param id ID
     * @param tenantId 租户ID
     * @param status 状态
     * @return 影响行数
     */
    @Override
    public int updateStatus(Long id, String tenantId, Integer status) {
        return apiKeyMapper.updateStatus(id, tenantId, status);
    }
}
