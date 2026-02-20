package com.xbk.knowledge.infrastructure.agent.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentRunContext;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunContextRepository;
import com.xbk.knowledge.infrastructure.dao.IAgentRunContextDao;
import com.xbk.knowledge.infrastructure.dao.po.AgentRunContextPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * AgentRunContext 仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class AgentRunContextRepositoryImpl implements AgentRunContextRepository {

    private final IAgentRunContextDao dao;

    /**
     * upsert。
     *
     * @param context 参数
     */
    @Override
    public void upsert(AgentRunContext context) {
        if (context == null || !StringUtils.hasText(context.getRunId())) {
            return;
        }
        if (context.getCreatedAt() == null) {
            context.setCreatedAt(LocalDateTime.now());
        }
        if (context.getUpdatedAt() == null) {
            context.setUpdatedAt(LocalDateTime.now());
        }
        dao.upsert(toPO(context));
    }

    /**
     * findByRunId。
     *
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentRunContext> findByRunId(String runId) {
        if (!StringUtils.hasText(runId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(toEntity(dao.findByRunId(runId)));
    }

    /**
     * updateStatus。
     *
     * @param runId 参数
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public int updateStatus(String runId, String status) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(status)) {
            return 0;
        }
        return dao.updateStatus(runId, status);
    }

    /**
     * 实体转持久化对象。
     */
    private AgentRunContextPO toPO(AgentRunContext context) {
        if (context == null) {
            return null;
        }
        return AgentRunContextPO.builder()
                .id(context.getId())
                .runId(context.getRunId())
                .status(context.getStatus())
                .snapshotJson(context.getSnapshotJson())
                .createdAt(context.getCreatedAt())
                .updatedAt(context.getUpdatedAt())
                .build();
    }

    /**
     * 持久化对象转实体。
     */
    private AgentRunContext toEntity(AgentRunContextPO po) {
        if (po == null) {
            return null;
        }
        return AgentRunContext.builder()
                .id(po.getId())
                .runId(po.getRunId())
                .status(po.getStatus())
                .snapshotJson(po.getSnapshotJson())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
