package com.xbk.knowledge.infrastructure.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.AgentRunContext;
import com.xbk.knowledge.domain.repository.agent.AgentRunContextRepository;
import com.xbk.knowledge.infrastructure.mapper.agent.AgentRunContextMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * AgentRunContext 仓储实现。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class AgentRunContextRepositoryImpl implements AgentRunContextRepository {

    private final AgentRunContextMapper mapper;

    /**
     * upsert。
     *
     * @param context 参数
     * @return 返回结果
     */
    @Override
    public int upsert(AgentRunContext context) {
        if (context == null || context.getOrgId() == null || !StringUtils.hasText(context.getRunId())) {
            return 0;
        }
        if (context.getCreatedAt() == null) {
            context.setCreatedAt(LocalDateTime.now());
        }
        if (context.getUpdatedAt() == null) {
            context.setUpdatedAt(LocalDateTime.now());
        }
        return mapper.upsert(context);
    }

    /**
     * findByRunId。
     *
     * @param orgId 参数
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentRunContext> findByRunId(Long orgId, String runId) {
        if (orgId == null || !StringUtils.hasText(runId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByRunId(orgId, runId));
    }

    /**
     * updateStatus。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public int updateStatus(Long orgId, String runId, String status) {
        if (orgId == null || !StringUtils.hasText(runId) || !StringUtils.hasText(status)) {
            return 0;
        }
        return mapper.updateStatus(orgId, runId, status);
    }
}

