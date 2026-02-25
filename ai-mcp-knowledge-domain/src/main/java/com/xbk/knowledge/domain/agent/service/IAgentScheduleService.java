package com.xbk.knowledge.domain.agent.service;

import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * AgentSchedule 领域服务。
 *
 * 职责：封装调度配置的核心业务规则。
 *
 * @author sxie
 */
public interface IAgentScheduleService {

    PageResult<AgentSchedule> queryPage(AgentSchedulePageQuery query);

    AgentSchedule queryById(AgentScheduleIdQuery query);

    AgentSchedule create(AgentSchedule schedule);

    AgentSchedule update(AgentSchedule schedule);

    AgentSchedule enable(Long id, Long operatorId);

    AgentSchedule disable(Long id, Long operatorId);

    /**
     * 绑定/回写关联的 XXL Job ID。
     * 
     * @param id 主键 ID。
     * @param xxlJobId XXL 任务 ID。
     * @param operatorId 操作人标识。
     * @return 更新后的调度配置实体。
     */
    AgentSchedule bindXxlJobId(Long id, Long xxlJobId, Long operatorId);

    void remove(Long id);
}
