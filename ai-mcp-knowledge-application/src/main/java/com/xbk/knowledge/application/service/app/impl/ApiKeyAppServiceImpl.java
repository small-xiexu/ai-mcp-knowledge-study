package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.model.identity.ApiKeyCreateResult;
import com.xbk.knowledge.application.service.app.ApiKeyAppService;
import com.xbk.knowledge.domain.model.entity.SysApiKey;
import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.model.vo.identity.ApiKeyPageQuery;
import com.xbk.knowledge.domain.repository.ApiKeyRepository;
import com.xbk.knowledge.domain.repository.IdentityRepository;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * API Key 管理应用服务实现。
 *
 * 职责：应用层用例实现，用于编排服务账号密钥流程。
 *
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class ApiKeyAppServiceImpl implements ApiKeyAppService {

    private final ApiKeyRepository apiKeyRepository;
    private final IdentityRepository identityRepository;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    /**
     * 分页查询 API Key。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<SysApiKey> queryPage(ApiKeyPageQuery query) {
        Integer offset = query.getOffset() == null ? 0 : query.getOffset();
        Integer pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        ApiKeyPageQuery normalizedQuery = new ApiKeyPageQuery(
                query.getTenantId(),
                query.getOwnerUserId(),
                query.getStatus(),
                offset,
                pageSize
        );
        List<SysApiKey> records = apiKeyRepository.findPage(normalizedQuery);
        long total = apiKeyRepository.count(normalizedQuery);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 创建 API Key。
     *
     * @param tenantId 租户ID
     * @param ownerUserId 归属用户ID
     * @param scopes 权限范围
     * @param expireAt 过期时间
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyCreateResult create(String tenantId, Long ownerUserId, List<String> scopes, LocalDateTime expireAt) {
        SysUser owner = identityRepository
                .findById(ownerUserId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + ownerUserId));
        if (!Objects.equals(owner.getTenantId(), tenantId)) {
            throw new BusinessException("不允许跨租户创建 API Key");
        }
        String accessKey = "ak_" + randomKey(24);
        String plainSecret = "sk_" + randomKey(32);
        String secretHash = bCryptPasswordEncoder.encode(plainSecret);
        String scopesJson = toJson(scopes);
        LocalDateTime now = LocalDateTime.now();
        SysApiKey apiKey = SysApiKey.builder()
                .tenantId(tenantId)
                .ownerUserId(ownerUserId)
                .accessKey(accessKey)
                .secretHash(secretHash)
                .scopes(scopesJson)
                .status(1)
                .expireAt(expireAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
        SysApiKey saved = apiKeyRepository.insert(apiKey);
        return ApiKeyCreateResult.builder()
                .apiKey(saved)
                .plainSecret(plainSecret)
                .build();
    }

    /**
     * 禁用 API Key。
     *
     * @param tenantId 租户ID
     * @param id API Key ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String tenantId, Long id) {
        SysApiKey apiKey = apiKeyRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("API Key 不存在，id: " + id));
        if (!Objects.equals(apiKey.getTenantId(), tenantId)) {
            throw new BusinessException("不允许跨租户禁用 API Key");
        }
        apiKeyRepository.updateStatus(id, tenantId, 0);
    }

    /**
     * 生成随机密钥片段。
     *
     * @param length 目标长度
     * @return 随机文本
     */
    private String randomKey(int length) {
        String raw = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        return raw.substring(0, length);
    }

    /**
     * 序列化作用域列表。
     *
     * @param scopes 权限范围
     * @return JSON 文本
     */
    private String toJson(List<String> scopes) {
        try {
            return objectMapper.writeValueAsString(scopes == null ? java.util.Collections.emptyList() : scopes);
        } catch (JsonProcessingException e) {
            throw new BusinessException("权限范围序列化失败");
        }
    }
}
