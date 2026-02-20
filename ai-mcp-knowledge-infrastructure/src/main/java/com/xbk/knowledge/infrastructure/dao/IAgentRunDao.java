package com.xbk.knowledge.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.infrastructure.dao.po.AgentRunPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AgentRun DAO（通过 XML 承载 SQL）。
 
  * @author xiexu
  */
@Mapper
public interface IAgentRunDao extends BaseMapper<AgentRunPO> {

    int insertRun(AgentRunPO run);

    int updateStatusAndMetrics(AgentRunPO run);

    int updateStatus(@Param("runId") String runId,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage,
                     @Param("endedAt") java.time.LocalDateTime endedAt);

    AgentRunPO findByRunId(@Param("runId") String runId);

    int incrementToolCallCount(@Param("runId") String runId, @Param("delta") int delta);

    int incrementToolDeniedCount(@Param("runId") String runId, @Param("delta") int delta);
}
