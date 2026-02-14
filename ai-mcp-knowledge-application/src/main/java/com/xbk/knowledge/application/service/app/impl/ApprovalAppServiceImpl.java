package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.app.ApprovalAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.support.contract.PlatformContractV1OutputSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport.ResolvedRag;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.model.entity.agent.AgentRun;
import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.repository.AgentRunRepository;
import com.xbk.knowledge.domain.repository.AgentVersionRepository;
import com.xbk.knowledge.domain.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.repository.SysAuditEventRepository;
import com.xbk.knowledge.domain.service.IModelConfigService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xbk.knowledge.types.tool.ToolInvokeBypassContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审批应用服务实现。
 *
 * 实现要点（方式B）：
 * 1) 审批通过后平台执行一次已审批的工具调用（使用审批单快照 arguments）
 * 2) 将工具结果注入到模型上下文中，继续生成 PlatformContractV1 最终结果
 * 3) 更新 approval_request/agent_run/agent_run_context 状态，并写审计（工具调用审计由工具回调侧写入）
 *
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalAppServiceImpl implements ApprovalAppService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentRunContextRepository agentRunContextRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final IModelConfigService modelConfigService;
    private final ModelProviderFactory modelProviderFactory;
    private final ToolCallbackProvider toolCallbackProvider;
    private final IdentityContextService identityContextService;
    private final ObjectMapper objectMapper;
    private final PlatformContractV1OutputSupport outputSupport;
    private final SysAuditEventRepository sysAuditEventRepository;
    private final AgentRagGovernanceSupport ragGovernanceSupport;

    @Override
    public PageResult<ApprovalRequest> list(Long orgId, String status, int offset, int pageSize) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        List<ApprovalRequest> list = approvalRequestRepository.list(orgId, status, safeOffset, safeSize);
        long total = approvalRequestRepository.count(orgId, status);
        int pageNum = safeSize == 0 ? 1 : (safeOffset / safeSize) + 1;
        return PageResult.of(list, total, pageNum, safeSize);
    }

    @Override
    public ApprovalRequest get(Long orgId, Long id) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        return approvalRequestRepository.findById(orgId, id)
                .orElseThrow(() -> new NotFoundException("审批单不存在，id=" + id));
    }

    @Override
    public PlatformContractV1 approve(Long orgId, Long id, String decisionComment) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        if (orgId == null || id == null) {
            throw new IllegalArgumentException("orgId/id 不能为空");
        }
        long start = System.currentTimeMillis();
        Long approverId = identityContextService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        ApprovalRequest req = approvalRequestRepository.findById(orgId, id)
                .orElseThrow(() -> new NotFoundException("审批单不存在，id=" + id));
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new BusinessException("审批单非待审批状态，不可审批，status=" + req.getStatus());
        }
        if (req.getExpireAt() != null && req.getExpireAt().isBefore(now)) {
            throw new BusinessException("审批单已过期，不可审批，expireAt=" + req.getExpireAt());
        }
        if (!StringUtils.hasText(req.getRunId()) || !StringUtils.hasText(req.getToolKey())) {
            throw new BusinessException("审批单缺少 runId/toolKey，不可续跑");
        }

        // 先标记 APPROVED，确保工具回调侧门禁能识别为已通过
        int updated = approvalRequestRepository.markApproved(orgId, id, approverId, decisionComment, now);
        if (updated <= 0) {
            throw new BusinessException("审批状态更新失败（可能已被处理），id=" + id);
        }
        recordApprovalAudit(orgId, req.getRunId(), id, "APPROVED", now, null);

        AgentRun run = agentRunRepository.findByRunId(orgId, req.getRunId())
                .orElseThrow(() -> new NotFoundException("关联 run 不存在，runId=" + req.getRunId()));
        agentRunRepository.updateStatus(orgId, req.getRunId(), "RUNNING", null, null);

        AgentVersion version = agentVersionRepository.findById(new AgentVersionIdQuery(orgId, req.getAgentVersionId()))
                .orElseThrow(() -> new NotFoundException("关联 AgentVersion 不存在，id=" + req.getAgentVersionId()));
        ModelConfig model = resolveModelForVersion(orgId, version);

        // 执行工具（使用 toolKey 精确定位回调）
        String toolResult = executeApprovedTool(req.getToolKey(), req.getArgumentsSnapshotJson(), req.getRunId());

        // 继续调用模型（不启用工具，避免二次工具调用），强制输出 v1 JSON 并解析/修复
        ContinuedOutput continued = continueRunByModel(version, model, run.getSessionId(), run.getAgentCode(), run.getRunId(), run.getOrgId(), run.getAgentVersionId(), toolResult);

        long costMs = System.currentTimeMillis() - start;
        agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                .runId(run.getRunId())
                .orgId(orgId)
                .status("SUCCESS")
                .modelIdUsed(model == null ? null : model.getId())
                .modelNameUsed(model == null ? null : model.getModelName())
                .totalTokens(null)
                .toolCallCount(null)
                .toolDeniedCount(null)
                .repairAttempts(null)
                .costMs(costMs)
                .errorMessage(null)
                .endedAt(LocalDateTime.now())
                .build());

        try {
            agentRunContextRepository.updateStatus(orgId, run.getRunId(), "RESUMED");
        } catch (Exception e) {
            log.warn("更新 agent_run_context 状态失败，runId: {}", run.getRunId(), e);
        }

        return PlatformContractV1.builder()
                .meta(PlatformContractV1.Meta.builder()
                        .runId(run.getRunId())
                        .agentCode(run.getAgentCode())
                        .agentVersionId(version.getId())
                        .agentVersionNo(version.getVersionNo())
                        .orgId(orgId)
                        .modelUsed(model == null ? null : model.getModelName())
                        .costMs(costMs)
                        .repairAttempts(0)
                        .build())
                .status("SUCCESS")
                .answer(continued == null || continued.contract == null ? "" : continued.contract.getAnswer())
                .uncertainty(continued == null || continued.contract == null ? "" : continued.contract.getUncertainty())
                .citations(continued == null || continued.contract == null ? List.of() : continued.contract.getCitations())
                .toolCalls(continued == null || continued.contract == null ? List.of() : continued.contract.getToolCalls())
                .actionsNext(continued == null || continued.contract == null ? List.of() : continued.contract.getActionsNext())
                .build();
    }

    @Override
    public ApprovalRequest reject(Long orgId, Long id, String decisionComment) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        if (orgId == null || id == null) {
            throw new IllegalArgumentException("orgId/id 不能为空");
        }
        Long approverId = identityContextService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        ApprovalRequest req = approvalRequestRepository.findById(orgId, id)
                .orElseThrow(() -> new NotFoundException("审批单不存在，id=" + id));
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new BusinessException("审批单非待审批状态，不可拒绝，status=" + req.getStatus());
        }
        int updated = approvalRequestRepository.markRejected(orgId, id, approverId, decisionComment, now);
        if (updated <= 0) {
            throw new BusinessException("拒绝审批失败（可能已被处理），id=" + id);
        }
        recordApprovalAudit(orgId, req.getRunId(), id, "REJECTED", now, decisionComment);

        if (StringUtils.hasText(req.getRunId())) {
            agentRunRepository.updateStatus(orgId, req.getRunId(), "CANCELLED", "审批拒绝", LocalDateTime.now());
            try {
                agentRunContextRepository.updateStatus(orgId, req.getRunId(), "EXPIRED");
            } catch (Exception e) {
                log.warn("更新 agent_run_context 状态失败（reject），runId: {}", req.getRunId(), e);
            }
        }
        return approvalRequestRepository.findById(orgId, id)
                .orElseThrow(() -> new NotFoundException("审批单不存在，id=" + id));
    }

    private void recordApprovalAudit(Long orgId,
                                     String runId,
                                     Long approvalId,
                                     String action,
                                     LocalDateTime occurredAt,
                                     String errorMessage) {
        if (sysAuditEventRepository == null || approvalId == null) {
            return;
        }
        try {
            OrgContext ctx = OrgContextHolder.get();
            Long operatorId = ctx == null ? null : ctx.operatorUserId();
            Long operatorOrgId = ctx == null ? null : ctx.operatorOrgId();
            String operatorType = operatorId == null ? "system" : "user";

            SysAuditEvent event = SysAuditEvent.builder()
                    .operatorId(operatorId)
                    .operatorOrgId(operatorOrgId)
                    .operatorType(operatorType)
                    .eventType("TOOL_APPROVAL")
                    .resourceType("approval_request")
                    .resourceId(String.valueOf(approvalId))
                    .resourceOrgId(orgId)
                    .action(action)
                    .requestId(runId)
                    .sourceIp(resolveSourceIp())
                    .userAgent(resolveUserAgent())
                    .result(errorMessage == null ? 1 : 0)
                    .errorMessage(errorMessage)
                    .costMs(0L)
                    .occurredAt(occurredAt == null ? LocalDateTime.now() : occurredAt)
                    .build();
            sysAuditEventRepository.insert(event);
        } catch (Exception e) {
            log.warn("写入审批审计失败，approvalId: {}, action: {}", approvalId, action, e);
        }
    }

    private String resolveSourceIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null || attrs.getRequest() == null) {
                return null;
            }
            jakarta.servlet.http.HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                int idx = forwarded.indexOf(',');
                return idx > 0 ? forwarded.substring(0, idx).trim() : forwarded.trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null || attrs.getRequest() == null) {
                return null;
            }
            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

    private String executeApprovedTool(String toolKey, String argumentsSnapshotJson, String runId) {
        if (toolCallbackProvider == null) {
            throw new BusinessException("工具执行能力不可用（toolCallbackProvider 未注入）");
        }
        ToolCallback tool = findToolByKey(toolKey)
                .orElseThrow(() -> new NotFoundException("未找到目标工具回调，toolKey=" + toolKey));

        // 强制 runId 贯穿工具调用日志与审计
        String previousTraceId = MDC.get(TraceIdUtils.TRACE_ID_KEY);
        MDC.put(TraceIdUtils.TRACE_ID_KEY, runId);
        ToolInvokeBypassContextHolder.enable();
        try {
            String args = StringUtils.hasText(argumentsSnapshotJson) ? argumentsSnapshotJson : "{}";
            return tool.call(args);
        } finally {
            ToolInvokeBypassContextHolder.clear();
            if (previousTraceId == null) {
                MDC.remove(TraceIdUtils.TRACE_ID_KEY);
            } else {
                MDC.put(TraceIdUtils.TRACE_ID_KEY, previousTraceId);
            }
        }
    }

    private Optional<ToolCallback> findToolByKey(String toolKey) {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        if (callbacks == null || callbacks.length == 0) {
            return Optional.empty();
        }
        for (ToolCallback cb : callbacks) {
            if (cb == null) {
                continue;
            }
            if (cb instanceof com.xbk.knowledge.types.tool.ToolKeyAware aware) {
                if (toolKey.equals(aware.toolKey())) {
                    return Optional.of(cb);
                }
            }
        }
        return Optional.empty();
    }

    private ContinuedOutput continueRunByModel(AgentVersion version,
                                              ModelConfig model,
                                              Long sessionId,
                                              String agentCode,
                                              String runId,
                                              Long orgId,
                                              Long agentVersionId,
                                              String toolResult) {
        if (version == null) {
            throw new BusinessException("AgentVersion 不能为空");
        }
        if (model == null) {
            throw new BusinessException("未找到可用模型");
        }
        ChatClient chatClient = modelProviderFactory.createChatClient(model);

        String system = version.getSystemPromptSnapshot();
        if (!StringUtils.hasText(system)) {
            system = "";
        }
        String injected = buildToolResultInjection(toolResult);

        String user = "";
        String ragTagsJson = null;
        try {
            AgentRunContextSnapshot snap = loadSnapshotFromRunContext(orgId, runId);
            if (snap != null && StringUtils.hasText(snap.content)) {
                user = snap.content;
            }
            if (snap != null && StringUtils.hasText(snap.ragTagsJson)) {
                ragTagsJson = snap.ragTagsJson;
            }
        } catch (Exception e) {
            // 快照缺失不阻断：兜底使用空输入
            log.warn("读取 run_context_snapshot 失败，runId: {}", runId, e);
        }

        ResolvedRag rag = ragGovernanceSupport == null ? null : ragGovernanceSupport.resolve(version, ragTagsJson, user);
        Prompt prompt;
        if (rag != null && rag.documents() != null && !rag.documents().isEmpty()) {
            String ragText = formatRagDocuments(rag.documents());
            String ragSystem = """
                    你可以参考以下【参考文档】继续回答用户问题。
                    如果【参考文档】中找不到答案，请在 uncertainty 字段明确说明不确定，不要编造。

                    【参考文档】
                    """ + ragText;
            prompt = new Prompt(
                    new SystemMessage(system),
                    new SystemMessage(ragSystem),
                    new SystemMessage(injected),
                    new SystemMessage(outputSupport.contractInstruction()),
                    new UserMessage(user)
            );
        } else {
            prompt = new Prompt(
                    new SystemMessage(system),
                    new SystemMessage(injected),
                    new SystemMessage(outputSupport.contractInstruction()),
                    new UserMessage(user)
            );
        }

        org.springframework.ai.chat.model.ChatResponse resp = chatClient.prompt(prompt).call().chatResponse();
        if (resp == null || resp.getResult() == null || resp.getResult().getOutput() == null) {
            throw new BusinessException("模型未返回可用输出");
        }
        String text = resp.getResult().getOutput().getText();
        PlatformContractV1 parsed = outputSupport.parseOrNull(text);
        if (parsed != null) {
            applyRagOverrides(parsed, rag);
            return new ContinuedOutput(parsed, 0);
        }

        int maxRetry = version.getRepairRetryTimes() == null ? 2 : version.getRepairRetryTimes();
        String current = text == null ? "" : text;
        for (int i = 1; i <= maxRetry; i++) {
            String repaired = repairOnce(chatClient, current);
            PlatformContractV1 p = outputSupport.parseOrNull(repaired);
            if (p != null) {
                applyRagOverrides(p, rag);
                return new ContinuedOutput(p, i);
            }
            current = repaired;
        }
        throw new BusinessException("审批续跑输出无法解析为 PlatformContractV1 JSON");
    }

    private String formatRagDocuments(List<org.springframework.ai.document.Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (org.springframework.ai.document.Document d : docs) {
            if (d == null) {
                continue;
            }
            idx++;
            sb.append("### Doc ").append(idx).append('\n');
            if (d.getMetadata() != null && d.getMetadata().get("knowledge") != null) {
                sb.append("tag: ").append(String.valueOf(d.getMetadata().get("knowledge"))).append('\n');
            }
            String text = d.getText() == null ? "" : d.getText();
            if (text.length() > 1600) {
                text = text.substring(0, 1600);
            }
            sb.append(text).append('\n');
            sb.append('\n');
            if (idx >= 5) {
                break;
            }
        }
        return sb.toString();
    }

    private void applyRagOverrides(PlatformContractV1 contract, ResolvedRag rag) {
        if (contract == null || rag == null) {
            return;
        }
        if (rag.citations() != null && !rag.citations().isEmpty()) {
            contract.setCitations(rag.citations());
        }
        if (rag.droppedTags() != null && !rag.droppedTags().isEmpty()) {
            List<String> actions = contract.getActionsNext();
            if (actions == null) {
                actions = new java.util.ArrayList<>();
                contract.setActionsNext(actions);
            }
            actions.add("RAG 标签未在白名单内，已忽略: " + String.join(",", rag.droppedTags()));
        }
    }

    private String repairOnce(ChatClient client, String invalidOutput) {
        String safe = invalidOutput == null ? "" : invalidOutput;
        Prompt prompt = new Prompt(
                new SystemMessage("你是 JSON 修复器。你必须仅输出合法 JSON，不要输出任何额外文字。"),
                new SystemMessage(outputSupport.contractInstruction()),
                new UserMessage("请将以下内容修复为符合要求的 JSON：\n" + safe)
        );
        org.springframework.ai.chat.model.ChatResponse resp = client.prompt(prompt).call().chatResponse();
        if (resp == null || resp.getResult() == null || resp.getResult().getOutput() == null) {
            return "";
        }
        String text = resp.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private String buildToolResultInjection(String toolResult) {
        String content = toolResult == null ? "" : toolResult;
        if (content.length() > 8000) {
            content = content.substring(0, 8000);
        }
        return "已执行并通过审批的工具调用结果如下（仅供继续推理，不再触发工具调用）:\n" + content;
    }

    private AgentRunContextSnapshot loadSnapshotFromRunContext(Long orgId, String runId) {
        if (agentRunContextRepository == null || orgId == null || !StringUtils.hasText(runId)) {
            return null;
        }
        return agentRunContextRepository.findByRunId(orgId, runId)
                .map(ctx -> {
                    if (ctx == null || !StringUtils.hasText(ctx.getSnapshotJson())) {
                        return null;
                    }
                    try {
                        Map<String, Object> map = objectMapper.readValue(ctx.getSnapshotJson(), new TypeReference<Map<String, Object>>() {});
                        String content = map == null ? null : String.valueOf(map.get("content"));
                        String ragTagsJson = map == null ? null : (map.get("ragTagsJson") == null ? null : String.valueOf(map.get("ragTagsJson")));
                        return new AgentRunContextSnapshot(content, ragTagsJson);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    private ModelConfig resolveModelForVersion(Long orgId, AgentVersion version) {
        if (version == null) {
            return null;
        }
        if ("FIXED_MODEL".equalsIgnoreCase(version.getModelStrategyType())) {
            if (version.getFixedModelId() == null) {
                throw new BusinessException("FIXED_MODEL 策略下 fixedModelId 不能为空");
            }
            ModelConfig model = modelConfigService.queryModelConfigById(new com.xbk.knowledge.domain.model.vo.common.IdQuery(version.getFixedModelId()));
            if (model != null && model.getOrgId() != null && orgId != null && !orgId.equals(model.getOrgId())) {
                throw new BusinessException("模型不属于当前组织，modelId: " + model.getId());
            }
            return model;
        }
        List<ModelConfig> enabled = modelConfigService.queryEnabledModels(new com.xbk.knowledge.domain.model.vo.common.EnabledQuery(true));
        if (enabled == null || enabled.isEmpty()) {
            throw new BusinessException("未配置可用模型");
        }
        return enabled.stream()
                .filter(m -> m != null && m.getOrgId() != null && orgId != null && orgId.equals(m.getOrgId()))
                .max(Comparator.comparingInt(m -> m.getPriority() == null ? 0 : m.getPriority()))
                .orElseThrow(() -> new BusinessException("当前组织未配置可用模型"));
    }

    private static final class AgentRunContextSnapshot {
        private final String content;
        private final String ragTagsJson;

        private AgentRunContextSnapshot(String content, String ragTagsJson) {
            this.content = content;
            this.ragTagsJson = ragTagsJson;
        }
    }

    private static final class ContinuedOutput {
        private final PlatformContractV1 contract;
        private final int repairAttempts;

        private ContinuedOutput(PlatformContractV1 contract, int repairAttempts) {
            this.contract = contract;
            this.repairAttempts = repairAttempts;
        }
    }
}
