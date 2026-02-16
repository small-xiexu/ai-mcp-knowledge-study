package com.xbk.knowledge.infrastructure.repository.tool;

import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import com.xbk.knowledge.domain.model.vo.tool.ToolPolicyPageQuery;
import com.xbk.knowledge.domain.repository.tool.ToolPolicyRepository;
import com.xbk.knowledge.infrastructure.mapper.tool.ToolPolicyMapper;
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

    /**
     * findEnabled。
     *
     * @param orgId 参数
     * @param toolKey 参数
     * @return 返回结果
     */
    @Override
    public Optional<ToolPolicy> findEnabled(Long orgId, String toolKey) {
        if (orgId == null || !StringUtils.hasText(toolKey)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findEnabled(orgId, toolKey));
    }

    /**
     * findById。
     *
     * @param orgId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Optional<ToolPolicy> findById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(orgId, id));
    }

    /**
     * findByToolKey。
     *
     * @param orgId 参数
     * @param toolKey 参数
     * @return 返回结果
     */
    @Override
    public Optional<ToolPolicy> findByToolKey(Long orgId, String toolKey) {
        if (orgId == null || !StringUtils.hasText(toolKey)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByToolKey(orgId, toolKey));
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<ToolPolicy> findPage(ToolPolicyPageQuery query) {
        if (query == null || query.orgId() == null) {
            return Collections.emptyList();
        }
        return mapper.findPage(query);
    }

    /**
     * count。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long count(ToolPolicyPageQuery query) {
        if (query == null || query.orgId() == null) {
            return 0;
        }
        return mapper.count(query);
    }

    /**
     * insert。
     *
     * @param policy 参数
     * @return 返回结果
     */
    @Override
    public ToolPolicy insert(ToolPolicy policy) {
        if (policy == null) {
            return null;
        }
        mapper.insertPolicy(policy);
        return policy;
    }

    /**
     * update。
     *
     * @param policy 参数
     * @return 返回结果
     */
    @Override
    public int update(ToolPolicy policy) {
        if (policy == null || policy.getOrgId() == null || policy.getId() == null) {
            return 0;
        }
        return mapper.updatePolicy(policy);
    }

    /**
     * updateEnabled。
     *
     * @param orgId 参数
     * @param id 参数
     * @param enabled 参数
     * @return 返回结果
     */
    @Override
    public int updateEnabled(Long orgId, Long id, Integer enabled) {
        if (orgId == null || id == null || enabled == null) {
            return 0;
        }
        return mapper.updateEnabled(orgId, id, enabled);
    }

    /**
     * deleteById。
     *
     * @param orgId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public int deleteById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return 0;
        }
        return mapper.deleteById(orgId, id);
    }
}
