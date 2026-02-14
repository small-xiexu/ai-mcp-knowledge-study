package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.agent.AgentRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AgentRun Mapper（通过 XML 承载 SQL）。
 */
@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRun> {

    int insertRun(AgentRun run);

    int updateStatusAndMetrics(AgentRun run);

    int updateStatus(@Param("orgId") Long orgId,
                     @Param("runId") String runId,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage,
                     @Param("endedAt") java.time.LocalDateTime endedAt);

    AgentRun findByRunId(@Param("orgId") Long orgId, @Param("runId") String runId);

    int incrementToolCallCount(@Param("orgId") Long orgId, @Param("runId") String runId, @Param("delta") int delta);

    int incrementToolDeniedCount(@Param("orgId") Long orgId, @Param("runId") String runId, @Param("delta") int delta);
}
