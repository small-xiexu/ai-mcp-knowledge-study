package com.xbk.knowledge.infrastructure.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.agent.AgentRunContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AgentRunContext Mapper（通过 XML 承载 SQL）。
 *
 * @author xiexu
 */
@Mapper
public interface AgentRunContextMapper extends BaseMapper<AgentRunContext> {

    int upsert(AgentRunContext context);

    AgentRunContext findByRunId(@Param("orgId") Long orgId, @Param("runId") String runId);

    int updateStatus(@Param("orgId") Long orgId, @Param("runId") String runId, @Param("status") String status);
}

