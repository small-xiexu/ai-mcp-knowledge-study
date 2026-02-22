package com.xbk.knowledge.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.infrastructure.dao.po.AgentSchedulePO;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AgentSchedule DAO（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IAgentScheduleDao extends BaseMapper<AgentSchedulePO> {

    int insertSchedule(AgentSchedulePO schedule);

    int updateSchedule(AgentSchedulePO schedule);

    int updateEnabled(@Param("id") Long id,
                      @Param("enabled") Boolean enabled,
                      @Param("updatedBy") Long updatedBy);

    int updateXxlJobId(@Param("id") Long id,
                       @Param("xxlJobId") Long xxlJobId,
                       @Param("updatedBy") Long updatedBy);

    int deleteById(@Param("id") Long id);

    int deleteByAgentId(@Param("agentId") Long agentId);

    AgentSchedulePO findById(AgentScheduleIdQuery query);

    List<AgentSchedulePO> listByAgentId(@Param("agentId") Long agentId);

    long countByAgentIdAndScheduleName(@Param("agentId") Long agentId,
                                       @Param("scheduleName") String scheduleName,
                                       @Param("excludeId") Long excludeId);

    long count(AgentSchedulePageQuery query);

    List<AgentSchedulePO> findPage(AgentSchedulePageQuery query);
}
