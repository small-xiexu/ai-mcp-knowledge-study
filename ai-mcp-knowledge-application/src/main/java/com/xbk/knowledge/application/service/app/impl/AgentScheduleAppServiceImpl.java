package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.app.AgentAppService;
import com.xbk.knowledge.application.service.app.AgentScheduleAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.XxlJobAppService;
import com.xbk.knowledge.application.support.xxl.XxlJobIdParser;
import com.xbk.knowledge.domain.job.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;
import com.xbk.knowledge.domain.agent.service.IAgentScheduleService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * AgentSchedule 应用服务实现。
 *
 * 关键点：
 * - schedule 落库后创建/更新 xxl-job，并将 jobId 回写到 schedule.xxl_job_id
 * - enable/disable 同步 start/stop xxl-job
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentScheduleAppServiceImpl implements AgentScheduleAppService {

    private static final String XXL_HANDLER = "agentScheduleHandler";

    private final IAgentScheduleService agentScheduleService;
    private final AgentAppService agentAppService;
    private final IdentityContextService identityContextService;
    private final XxlJobAppService xxlJobAppService;
    private final ObjectMapper objectMapper;

    /**
     * queryPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public PageResult<AgentSchedule> queryPage(AgentSchedulePageQuery query) {
        return agentScheduleService.queryPage(query);
    }

    /**
     * queryById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public AgentSchedule queryById(AgentScheduleIdQuery query) {
        return agentScheduleService.queryById(query);
    }

    /**
     * create。
     *
     * @param schedule 参数
     * @param agentCode 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSchedule create(AgentSchedule schedule, String agentCode) {
        if (schedule == null) {
            throw new IllegalArgumentException("schedule 不能为空");
        }
        if (!StringUtils.hasText(agentCode)) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        Long userId = identityContextService.getCurrentUserId();

        Agent agent = agentAppService.queryByCode(new AgentCodeQuery(agentCode));
        schedule.setAgentId(agent.getId());
        schedule.setCreatedBy(userId);
        schedule.setUpdatedBy(userId);

        AgentSchedule created = agentScheduleService.create(schedule);

        // 创建/更新 xxl-job
        Long jobId = ensureXxlJob(created, agentCode, true);
        if (jobId != null) {
            created = agentScheduleService.bindXxlJobId(created.getId(), jobId, userId);
        }
        return created;
    }

    /**
     * update。
     *
     * @param schedule 参数
     * @param agentCode 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSchedule update(AgentSchedule schedule, String agentCode) {
        if (schedule == null || schedule.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        if (!StringUtils.hasText(agentCode)) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        Long userId = identityContextService.getCurrentUserId();

        Agent agent = agentAppService.queryByCode(new AgentCodeQuery(agentCode));
        schedule.setAgentId(agent.getId());
        schedule.setUpdatedBy(userId);

        AgentSchedule updated = agentScheduleService.update(schedule);

        Long jobId = ensureXxlJob(updated, agentCode, false);
        if (jobId != null) {
            updated = agentScheduleService.bindXxlJobId(updated.getId(), jobId, userId);
        }
        return updated;
    }

    /**
     * enable。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSchedule enable(Long id) {
        Long userId = identityContextService.getCurrentUserId();
        AgentSchedule schedule = agentScheduleService.enable(id, userId);
        if (schedule.getXxlJobId() == null) {
            // 未绑定 job 时，启用视为创建 job 并启动
            String agentCode = resolveAgentCode(schedule.getAgentId());
            Long jobId = ensureXxlJob(schedule, agentCode, true);
            schedule = agentScheduleService.bindXxlJobId(id, jobId, userId);
        } else {
            xxlJobAppService.startJob(schedule.getXxlJobId());
        }
        return schedule;
    }

    /**
     * disable。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSchedule disable(Long id) {
        Long userId = identityContextService.getCurrentUserId();
        AgentSchedule schedule = agentScheduleService.disable(id, userId);
        if (schedule.getXxlJobId() != null) {
            xxlJobAppService.stopJob(schedule.getXxlJobId());
        }
        return schedule;
    }

    /**
     * remove。
     *
     * @param id 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        AgentSchedule existed = agentScheduleService.queryById(new AgentScheduleIdQuery(id));
        if (existed.getXxlJobId() != null) {
            try {
                xxlJobAppService.removeJob(existed.getXxlJobId());
            } catch (Exception e) {
                log.warn("删除 xxl-job 失败（忽略并继续删除 schedule），jobId={}", existed.getXxlJobId(), e);
            }
        }
        agentScheduleService.remove(id);
    }

    private String resolveAgentCode(Long agentId) {
        // 当前控制面只提供按 code 查；这里做最小兜底：不做反查，使用 agentId 作为标识在 jobDesc 中会降低可读性
        // 生产建议补 agentId->agentCode 查询接口或在 schedule 表冗余 agent_code
        return "agentId=" + agentId;
    }

    private Long ensureXxlJob(AgentSchedule schedule, String agentCode, boolean createIfMissing) {
        if (schedule == null || schedule.getId() == null) {
            return null;
        }
        Long scheduleId = schedule.getId();
        String executorParam = buildExecutorParam(scheduleId);

        if (schedule.getXxlJobId() == null) {
            if (!createIfMissing) {
                // update 场景若缺 jobId，则创建
            }
            XxlJobInfo jobInfo = XxlJobInfo.builder()
                    .jobDesc("AgentSchedule: " + agentCode + " (scheduleId=" + scheduleId + ")")
                    .author("platform")
                    .scheduleType("CRON")
                    .scheduleConf(schedule.getCron())
                    .executorHandler(XXL_HANDLER)
                    .executorParam(executorParam)
                    .executorRouteStrategy("FIRST")
                    .executorBlockStrategy("SERIAL_EXECUTION")
                    .executorTimeout(0)
                    .executorFailRetryCount(0)
                    .glueType("BEAN")
                    .build();
            String result = xxlJobAppService.createJob(jobInfo);
            Long jobId = XxlJobIdParser.parseJobIdOrNull(result);
            if (jobId == null) {
                throw new BusinessException("创建 xxl-job 失败，result=" + result);
            }
            schedule.setXxlJobId(jobId);
            // schedule.xxl_job_id 的回写由上层调用 bindXxlJobId 负责
            if (Boolean.TRUE.equals(schedule.getEnabled())) {
                xxlJobAppService.startJob(jobId);
            } else {
                xxlJobAppService.stopJob(jobId);
            }
            return jobId;
        }

        // 已存在 job：更新 cron 与 param
        XxlJobInfo jobInfo = XxlJobInfo.builder()
                .id(schedule.getXxlJobId())
                .jobDesc("AgentSchedule: " + agentCode + " (scheduleId=" + scheduleId + ")")
                .author("platform")
                .scheduleType("CRON")
                .scheduleConf(schedule.getCron())
                .executorHandler(XXL_HANDLER)
                .executorParam(executorParam)
                .executorRouteStrategy("FIRST")
                .executorBlockStrategy("SERIAL_EXECUTION")
                .executorTimeout(0)
                .executorFailRetryCount(0)
                .glueType("BEAN")
                .build();
        xxlJobAppService.updateJob(jobInfo);
        if (Boolean.TRUE.equals(schedule.getEnabled())) {
            xxlJobAppService.startJob(schedule.getXxlJobId());
        } else {
            xxlJobAppService.stopJob(schedule.getXxlJobId());
        }
        return schedule.getXxlJobId();
    }

    private String buildExecutorParam(Long scheduleId) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "scheduleId", scheduleId
            ));
        } catch (Exception e) {
            return "{\"scheduleId\":" + scheduleId + "}";
        }
    }
}
