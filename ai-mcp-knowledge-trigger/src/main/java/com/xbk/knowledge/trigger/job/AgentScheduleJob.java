package com.xbk.knowledge.trigger.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.domain.model.entity.agent.AgentSchedule;
import com.xbk.knowledge.domain.model.vo.agent.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.repository.agent.AgentScheduleRepository;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.exception.BusinessException;
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
 * executorParam 约定（JSON）：
 * - orgId: number
 * - scheduleId: number
 *
 * 说明：
 * - 执行时必须取当前发布版本（由 AgentRuntimeAppService.runJob 保证）
 * - 若触发审批（PENDING_APPROVAL），按口径视为“Job 成功”（2A），并打印 approvalRequestId 供人工处理
 *
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentScheduleJob {

    private final ObjectMapper objectMapper;
    private final AgentScheduleRepository agentScheduleRepository;
    private final AgentRuntimeAppService agentRuntimeAppService;

    /**
     * execute。
     *
     */
    @XxlJob("agentScheduleHandler")
    public void execute() {
        String param = XxlJobHelper.getJobParam();
        if (!StringUtils.hasText(param)) {
            throw new BusinessException("executorParam 不能为空（需要 orgId/scheduleId）");
        }

        Long orgId = null;
        Long scheduleId = null;
        try {
            Map<String, Object> map = objectMapper.readValue(param, new TypeReference<Map<String, Object>>() {});
            Object o1 = map == null ? null : map.get("orgId");
            Object o2 = map == null ? null : map.get("scheduleId");
            orgId = o1 == null ? null : Long.valueOf(String.valueOf(o1));
            scheduleId = o2 == null ? null : Long.valueOf(String.valueOf(o2));
        } catch (Exception e) {
            throw new BusinessException("executorParam 解析失败，需要 JSON: {orgId,scheduleId}, param=" + param);
        }
        if (orgId == null || scheduleId == null) {
            throw new BusinessException("executorParam 缺少 orgId/scheduleId, param=" + param);
        }

        final Long orgIdFinal = orgId;
        final Long scheduleIdFinal = scheduleId;

        OrgContext previous = OrgContextHolder.get();
        try {
            OrgContextHolder.set(new OrgContext(
                    null,
                    orgIdFinal,
                    orgIdFinal,
                    false,
                    true
            ));

            AgentSchedule schedule = agentScheduleRepository.findById(new AgentScheduleIdQuery(orgIdFinal, scheduleIdFinal))
                    .orElseThrow(() -> new BusinessException("调度不存在，scheduleId=" + scheduleIdFinal));
            if (!Boolean.TRUE.equals(schedule.getEnabled())) {
                XxlJobHelper.log("schedule disabled, skip. scheduleId={}, orgId={}", scheduleIdFinal, orgIdFinal);
                return;
            }
            if (!StringUtils.hasText(schedule.getAgentCode())) {
                throw new BusinessException("schedule 缺少 agentCode（join 结果为空），scheduleId=" + scheduleIdFinal);
            }

            Payload payload = parsePayload(schedule.getPayloadTemplateJson());
            if (!StringUtils.hasText(payload.content)) {
                throw new BusinessException("payloadTemplateJson 缺少 content，scheduleId=" + scheduleIdFinal);
            }

            PlatformContractV1 result = agentRuntimeAppService.runJob(orgIdFinal, schedule.getAgentCode(), payload.content, payload.ragTagsJson);
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
        } finally {
            if (previous == null) {
                OrgContextHolder.clear();
            } else {
                OrgContextHolder.set(previous);
            }
        }
    }

    private Payload parsePayload(String payloadTemplateJson) {
        if (!StringUtils.hasText(payloadTemplateJson)) {
            return new Payload("", null);
        }
        try {
            Map<String, Object> map = objectMapper.readValue(payloadTemplateJson, new TypeReference<Map<String, Object>>() {});
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
