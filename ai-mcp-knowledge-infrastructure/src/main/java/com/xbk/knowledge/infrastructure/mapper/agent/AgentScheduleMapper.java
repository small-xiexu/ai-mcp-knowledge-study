package com.xbk.knowledge.infrastructure.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.agent.AgentSchedule;
import com.xbk.knowledge.domain.model.vo.agent.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentSchedulePageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AgentSchedule Mapper（通过 XML 承载 SQL）。
 
  * @author xiexu
  */
@Mapper
public interface AgentScheduleMapper extends BaseMapper<AgentSchedule> {

    int insertSchedule(AgentSchedule schedule);

    int updateSchedule(AgentSchedule schedule);

    int updateEnabled(@Param("orgId") Long orgId,
                      @Param("id") Long id,
                      @Param("enabled") Boolean enabled,
                      @Param("updatedBy") Long updatedBy);

    int updateXxlJobId(@Param("orgId") Long orgId,
                       @Param("id") Long id,
                       @Param("xxlJobId") Long xxlJobId,
                       @Param("updatedBy") Long updatedBy);

    int deleteById(@Param("orgId") Long orgId, @Param("id") Long id);

    AgentSchedule findById(AgentScheduleIdQuery query);

    AgentSchedule findByOrgIdAndAgentId(@Param("orgId") Long orgId, @Param("agentId") Long agentId);

    long count(AgentSchedulePageQuery query);

    List<AgentSchedule> findPage(AgentSchedulePageQuery query);
}

