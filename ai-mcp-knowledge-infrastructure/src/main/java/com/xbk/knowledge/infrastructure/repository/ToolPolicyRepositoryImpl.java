package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import com.xbk.knowledge.domain.repository.ToolPolicyRepository;
import com.xbk.knowledge.infrastructure.mapper.ToolPolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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
}

