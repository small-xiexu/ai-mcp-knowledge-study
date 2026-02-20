package com.xbk.knowledge.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.infrastructure.dao.po.AgentRunContextPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AgentRunContext DAO（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IAgentRunContextDao extends BaseMapper<AgentRunContextPO> {

    int upsert(AgentRunContextPO context);

    AgentRunContextPO findByRunId(@Param("runId") String runId);

    int updateStatus(@Param("runId") String runId, @Param("status") String status);
}

