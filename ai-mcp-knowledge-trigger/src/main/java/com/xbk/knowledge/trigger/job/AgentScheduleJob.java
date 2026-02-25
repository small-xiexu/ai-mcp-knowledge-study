package com.xbk.knowledge.trigger.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentScheduleRepository;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.json.JsonMapUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * AgentSchedule 执行任务。
 *
 * XXL-Job Handler: agentScheduleHandler
 *
 * executorParam 约定（JSON）
 * - scheduleId: number
 *
 * 说明：
 * - 执行时必须取当前发布版本（由 AgentRuntimeAppService.runJob 保证）
 * - 若触发审批（PENDING_APPROVAL），按口径视为“Job 成功”（2A），并打印 approvalRequestId 供人工处理
 *
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentScheduleJob {

    /**
     * JSON 序列化/反序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * Agent 调度仓储。
     */
    private final AgentScheduleRepository agentScheduleRepository;

    /**
     * Agent 运行时应用服务。
     */
    private final AgentRuntimeAppService agentRuntimeAppService;

    /**
     * 执行定时任务处理。
     */
    @XxlJob("agentScheduleHandler")
    public void execute() {
        String param = XxlJobHelper.getJobParam();
        if (!StringUtils.hasText(param)) {
            throw new BusinessException("executorParam 不能为空（需要 scheduleId）");
        }

        Long scheduleId = null;
        try {
            Map<String, Object> map = JsonMapUtils.readMap(objectMapper, param);
            Object o1 = map == null ? null : map.get("scheduleId");
            scheduleId = o1 == null ? null : Long.valueOf(String.valueOf(o1));
        } catch (Exception e) {
            throw new BusinessException("executorParam 解析失败，需要 JSON: {scheduleId}, param=" + param);
        }
        if (scheduleId == null) {
            throw new BusinessException("executorParam 缺少 scheduleId, param=" + param);
        }

        final Long scheduleIdFinal = scheduleId;

        AgentSchedule schedule = agentScheduleRepository.findById(new AgentScheduleIdQuery(scheduleIdFinal))
                .orElseThrow(() -> new BusinessException("调度不存在，scheduleId=" + scheduleIdFinal));
        if (!Boolean.TRUE.equals(schedule.getEnabled())) {
            XxlJobHelper.log("schedule disabled, skip. scheduleId={}", scheduleIdFinal);
            return;
        }
        if (!StringUtils.hasText(schedule.getAgentCode())) {
            throw new BusinessException("schedule 缺少 agentCode（join 结果为空），scheduleId=" + scheduleIdFinal);
        }

        Payload payload = parsePayload(schedule.getPayloadTemplateJson());
        if (!StringUtils.hasText(payload.content)) {
            throw new BusinessException("payloadTemplateJson 缺少 content，scheduleId=" + scheduleIdFinal);
        }

        PlatformContractV1 result = agentRuntimeAppService.runJob(schedule.getAgentCode(), payload.content, payload.ragTagsJson);
        if (result == null) {
            throw new BusinessException("Agent 运行返回为空");
        }
        if ("PENDING_APPROVAL".equalsIgnoreCase(result.getStatus())) {
            XxlJobHelper.log("approval required. runId={}, approvalRequestId={}, toolKey={}",
                    result.getMeta() == null ? null : result.getMeta().getRunId(),
                    result.getMeta() == null ? null : result.getMeta().getApprovalRequestId(),
                    result.getMeta() == null ? null : result.getMeta().getPendingToolKey());
            return;
        }
        if ("FAILED".equalsIgnoreCase(result.getStatus())) {
            String detail = result.getError() == null ? null : result.getError().getDetail();
            throw new BusinessException("Agent 运行失败，runId=" + (result.getMeta() == null ? null : result.getMeta().getRunId()) + ", detail=" + detail);
        }
        XxlJobHelper.log("agent schedule done. status={}, runId={}", result.getStatus(), result.getMeta() == null ? null : result.getMeta().getRunId());
    }

    /**
     * 解析调度任务负载。
     * 
     * @param payloadTemplateJson 载荷模板JSON。
     * @return 调度任务负载。
     */
    private Payload parsePayload(String payloadTemplateJson) {
        if (!StringUtils.hasText(payloadTemplateJson)) {
            return new Payload("", null);
        }
        try {
            Map<String, Object> map = JsonMapUtils.readMap(objectMapper, payloadTemplateJson);
            String content = map == null || map.get("content") == null ? "" : String.valueOf(map.get("content"));
            String ragTagsJson = map == null || map.get("ragTagsJson") == null ? null : String.valueOf(map.get("ragTagsJson"));
            return new Payload(content, ragTagsJson);
        } catch (Exception e) {
            // 允许用户直接填纯文本
            return new Payload(payloadTemplateJson, null);
        }
    }

    private record Payload(String content, String ragTagsJson) {
    }
}
