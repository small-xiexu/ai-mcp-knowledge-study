package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import com.xbk.knowledge.domain.model.vo.tool.ToolPolicyPageQuery;
import com.xbk.knowledge.domain.repository.ToolPolicyRepository;
import com.xbk.knowledge.infrastructure.mapper.ToolPolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * ToolPolicy 仓储实现。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class ToolPolicyRepositoryImpl implements ToolPolicyRepository {

    private final ToolPolicyMapper mapper;

    @Override
    public Optional<ToolPolicy> findEnabled(Long orgId, String toolKey) {
        if (orgId == null || !StringUtils.hasText(toolKey)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findEnabled(orgId, toolKey));
    }

    @Override
    public Optional<ToolPolicy> findById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(orgId, id));
    }

    @Override
    public Optional<ToolPolicy> findByToolKey(Long orgId, String toolKey) {
        if (orgId == null || !StringUtils.hasText(toolKey)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByToolKey(orgId, toolKey));
    }

    @Override
    public List<ToolPolicy> findPage(ToolPolicyPageQuery query) {
        if (query == null || query.orgId() == null) {
            return Collections.emptyList();
        }
        return mapper.findPage(query);
    }

    @Override
    public long count(ToolPolicyPageQuery query) {
        if (query == null || query.orgId() == null) {
            return 0;
        }
        return mapper.count(query);
    }

    @Override
    public ToolPolicy insert(ToolPolicy policy) {
        if (policy == null) {
            return null;
        }
        mapper.insertPolicy(policy);
        return policy;
    }

    @Override
    public int update(ToolPolicy policy) {
        if (policy == null || policy.getOrgId() == null || policy.getId() == null) {
            return 0;
        }
        return mapper.updatePolicy(policy);
    }

    @Override
    public int updateEnabled(Long orgId, Long id, Integer enabled) {
        if (orgId == null || id == null || enabled == null) {
            return 0;
        }
        return mapper.updateEnabled(orgId, id, enabled);
    }

    @Override
    public int deleteById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return 0;
        }
        return mapper.deleteById(orgId, id);
    }
}
