package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.app.ApprovalAppService;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.WorkflowRuntimeAppService;
import com.xbk.knowledge.application.support.contract.PlatformContractV1OutputSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport.ResolvedRag;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.agent.model.entity.AgentRun;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentVersionRepository;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.audit.adapter.repository.SysAuditEventRepository;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunContextRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunRepository;
import com.xbk.knowledge.domain.llm.service.IModelConfigService;
import com.xbk.knowledge.types.common.PageParamUtils;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.json.JsonMapUtils;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xbk.knowledge.types.tool.ToolKeyAware;
import com.xbk.knowledge.types.tool.ToolInvokeBypassContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审批应用服务实现。
 *
 * 实现要点（方式B）
 * 1、 审批通过后平台执行一次已审批的工具调用（使用审批单快照 arguments）
 * 2、 将工具结果注入到模型上下文中，继续生成 PlatformContractV1 最终结果
 * 3、 更新 approval_request/agent_run/agent_run_context 状态，并写审计（工具调用审计由工具回调侧写入）
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalAppServiceImpl implements ApprovalAppService {
    /**
     * 审批单仓储，用于审批查询与状态流转。
     */
    private final ApprovalRequestRepository approvalRequestRepository;

    /**
     * Agent 运行记录仓储，用于更新运行状态与指标。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * Agent 运行上下文仓储，用于续跑状态维护。
     */
    private final AgentRunContextRepository agentRunContextRepository;

    /**
     * AgentVersion 仓储，用于加载审批关联版本配置。
     */
    private final AgentVersionRepository agentVersionRepository;

    /**
     * 模型配置领域服务，用于解析续跑使用模型。
     */
    private final IModelConfigService modelConfigService;

    /**
     * ChatClient 装配服务，用于构建续跑调用客户端。
     */
    private final ChatClientAssemblyService chatClientAssemblyService;

    /**
     * 工具回调提供器，用于执行审批通过后的工具调用。
     */
    private final ToolCallbackProvider toolCallbackProvider;

    /**
     * 身份上下文服务，用于获取当前审批人身份。
     */
    private final IdentityContextService identityContextService;

    /**
     * JSON 序列化组件，用于快照与上下文 JSON 处理。
     */
    private final ObjectMapper objectMapper;

    /**
     * PlatformContract 输出支持组件，用于解析/修复模型 JSON 输出。
     */
    private final PlatformContractV1OutputSupport outputSupport;

    /**
     * 审计事件仓储，用于记录审批行为审计日志。
     */
    private final SysAuditEventRepository sysAuditEventRepository;

    /**
     * RAG 治理支持组件，用于补充续跑检索上下文。
     */
    private final AgentRagGovernanceSupport ragGovernanceSupport;

    /**
     * Workflow 运行时服务，用于审批后恢复工作流执行。
     */
    private final WorkflowRuntimeAppService workflowRuntimeAppService;

    /**
     * Workflow 运行记录仓储，用于查询审批关联 workflow run。
     */
    private final WorkflowRunRepository workflowRunRepository;

    /**
     * Workflow 运行上下文仓储，用于恢复执行上下文。
     */
    private final WorkflowRunContextRepository workflowRunContextRepository;

    /**
     * Agent 运行时服务，用于恢复 Planning 运行链路。
     */
    private final AgentRuntimeAppService agentRuntimeAppService;

    /**
     * 根据筛选条件查询审批列表。
     *
     * @param status 状态值
     * @param offset 分页偏移量
     * @param pageSize 分页大小
     * @return ApprovalRequest 分页数据
     */
    @Override
    public PageResult<ApprovalRequest> list(String status, int offset, int pageSize) {
        int safeOffset = PageParamUtils.normalizeOffset(offset);
        int safeSize = PageParamUtils.normalizePageSize(pageSize, 20, 200);
        List<ApprovalRequest> list = approvalRequestRepository.list(status, safeOffset, safeSize);
        long total = approvalRequestRepository.count(status);
        int pageNum = PageParamUtils.offsetToPageNum(safeOffset, safeSize);
        return PageResult.of(list, total, pageNum, safeSize);
    }

    /**
     * 查询审批。
     *
     * @param id 主键 ID
     * @return ApprovalRequest 详情
     */
    @Override
    public ApprovalRequest get(Long id) {
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("审批单不存在，id=" + id));
    }

    /**
     * 审批通过并触发续跑。
     *
     * @param id 主键 ID
     * @param decisionComment 审批意见
     * @return 审批通过后的平台协议结果
     */
    @Override
    public PlatformContractV1 approve(Long id, String decisionComment) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        long start = System.currentTimeMillis();
        Long approverId = identityContextService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        ApprovalRequest req = approvalRequestRepository.findById(id)
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
        int updated = approvalRequestRepository.markApproved(id, approverId, decisionComment, now);
        if (updated <= 0) {
            throw new BusinessException("审批状态更新失败（可能已被处理），id=" + id);
        }
        recordApprovalAudit(req.getRunId(), id, "APPROVED", now, null);

        if ("PLAN_EXECUTE".equalsIgnoreCase(req.getApprovalType())) {
            if (!StringUtils.hasText(req.getRunId())) {
                throw new BusinessException("PLAN_EXECUTE 缺少 runId，无法续跑");
            }
            return agentRuntimeAppService.resumePlannedRun(req.getRunId(), req.getId());
        }

        // 分支Agent 场景沿用旧逻辑；Workflow 场景交给 WorkflowRuntime 续跑
        if (req.getAgentVersionId() != null) {
            AgentRun run = agentRunRepository.findByRunId(req.getRunId())
                    .orElseThrow(() -> new NotFoundException("关联 run 不存在，runId=" + req.getRunId()));
            agentRunRepository.updateStatus(req.getRunId(), "RUNNING", null, null);

            AgentVersion version = agentVersionRepository.findById(new AgentVersionIdQuery(req.getAgentVersionId()))
                    .orElseThrow(() -> new NotFoundException("关联 AgentVersion 不存在，id=" + req.getAgentVersionId()));
            ModelConfig model = resolveModelForVersion(version);

            // 执行工具（使用 toolKey 精确定位回调）
            String toolResult = executeApprovedTool(req.getToolKey(), req.getArgumentsSnapshotJson(), req.getRunId());

            // 继续调用模型（不启用工具，避免二次工具调用），强制输出 v1 JSON 并解析/修复
            ContinuedOutput continued = continueRunByModel(version, model, run.getSessionId(), run.getAgentCode(), run.getRunId(), run.getAgentVersionId(), toolResult);

            long costMs = System.currentTimeMillis() - start;
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(run.getRunId())
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
                agentRunContextRepository.updateStatus(run.getRunId(), "RESUMED");
            } catch (Exception e) {
                log.warn("更新 agent_run_context 状态失败，runId: {}", run.getRunId(), e);
            }

            return PlatformContractV1.builder()
                    .meta(PlatformContractV1.Meta.builder()
                            .runId(run.getRunId())
                            .agentCode(run.getAgentCode())
                            .agentVersionId(version.getId())
                            .agentVersionNo(version.getVersionNo())
                            .modelUsed(model == null ? null : model.getModelName())
                            .costMs(costMs)
                            .repairAttempts(0)
                            .approvalRequestId(req.getId())
                            .build())
                    .status("SUCCESS")
                    .answer(continued == null || continued.contract == null ? "" : continued.contract.getAnswer())
                    .uncertainty(continued == null || continued.contract == null ? "" : continued.contract.getUncertainty())
                    .citations(continued == null || continued.contract == null ? List.of() : continued.contract.getCitations())
                    .toolCalls(continued == null || continued.contract == null ? List.of() : continued.contract.getToolCalls())
                    .actionsNext(continued == null || continued.contract == null ? List.of() : continued.contract.getActionsNext())
                    .build();
        }

        if (req.getWorkflowVersionId() != null) {
            // Workflow 续跑由 WorkflowRuntime 负责执行工具 + 继续执行图
            PlatformContractV1 result = workflowRuntimeAppService.resumeFromApproval(id);
            return result;
        }

        throw new BusinessException("审批单缺少 agent/workflow 归属信息，无法续跑");
    }

    /**
     * 拒绝审批请求并更新状态。
     *
     * @param id 主键 ID
     * @param decisionComment 审批意见
     * @return 更新后的审批单信息
     */
    @Override
    public ApprovalRequest reject(Long id, String decisionComment) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        Long approverId = identityContextService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        ApprovalRequest req = approvalRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("审批单不存在，id=" + id));
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new BusinessException("审批单非待审批状态，不可拒绝，status=" + req.getStatus());
        }
        int updated = approvalRequestRepository.markRejected(id, approverId, decisionComment, now);
        if (updated <= 0) {
            throw new BusinessException("拒绝审批失败（可能已被处理），id=" + id);
        }
        recordApprovalAudit(req.getRunId(), id, "REJECTED", now, decisionComment);

        if (StringUtils.hasText(req.getRunId())) {
            if (req.getAgentVersionId() != null) {
                agentRunRepository.updateStatus(req.getRunId(), "CANCELLED", "审批拒绝", LocalDateTime.now());
                try {
                    agentRunContextRepository.updateStatus(req.getRunId(), "EXPIRED");
                } catch (Exception e) {
                    log.warn("更新 agent_run_context 状态失败（reject），runId: {}", req.getRunId(), e);
                }
            } else if (req.getWorkflowVersionId() != null) {
                workflowRunRepository.updateStatus(req.getRunId(), "CANCELLED", "审批拒绝", LocalDateTime.now());
                try {
                    workflowRunContextRepository.updateStatus(req.getRunId(), "EXPIRED");
                } catch (Exception e) {
                    log.warn("更新 workflow_run_context 状态失败（reject），runId: {}", req.getRunId(), e);
                }
            }
        }
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("审批单不存在，id=" + id));
    }

    /**
     * 记录审批审计日志。
     * 
     * @param runId 运行ID。
     * @param approvalId 审批单ID。
     * @param action 审批动作。
     * @param occurredAt 发生时间。
     * @param errorMessage 错误信息。
     */
    private void recordApprovalAudit(String runId,
                                     Long approvalId,
                                     String action,
                                     LocalDateTime occurredAt,
                                     String errorMessage) {
        if (sysAuditEventRepository == null || approvalId == null) {
            return;
        }
        try {
            Long operatorId = identityContextService.getCurrentUserId();
            String operatorType = operatorId == null ? "system" : "user";

            SysAuditEvent event = SysAuditEvent.builder()
                    .operatorId(operatorId)
                    .operatorType(operatorType)
                    .eventType("TOOL_APPROVAL")
                    .resourceType("approval_request")
                    .resourceId(String.valueOf(approvalId))
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
            HttpServletRequest req = attrs.getRequest();
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

    /**
     * 执行已审批通过的工具调用。
     *
     * @param toolKey 工具标识
     * @param argumentsSnapshotJson 工具参数快照 JSON
     * @param runId 运行 ID
     * @return 工具调用输出内容
     */
    private String executeApprovedTool(String toolKey, String argumentsSnapshotJson, String runId) {
        if (toolCallbackProvider == null) {
            throw new BusinessException("工具执行能力不可用（toolCallbackProvider 未注入）");
        }
        ToolCallback tool = findToolByKey(toolKey)
                .orElseThrow(() -> new NotFoundException("未找到目标工具回调，toolKey=" + toolKey));

        // 使用 runId 作为 traceId 执行工具调用，自动管理 MDC 上下文
        return TraceIdUtils.runWithTraceId(runId, () -> {
            ToolInvokeBypassContextHolder.enable();
            try {
                String args = StringUtils.hasText(argumentsSnapshotJson) ? argumentsSnapshotJson : "{}";
                return tool.call(args);
            } finally {
                ToolInvokeBypassContextHolder.clear();
            }
        });
    }

    private Optional<ToolCallback> findToolByKey(String toolKey) {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        if (callbacks == null) {
            return Optional.empty();
        }
        for (ToolCallback cb : callbacks) {
            if (cb == null) {
                continue;
            }
            if (cb instanceof ToolKeyAware aware) {
                if (toolKey.equals(aware.toolKey())) {
                    return Optional.of(cb);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 基于模型继续执行审批后的运行流程。
     *
     * @param version 工作流版本
     * @param model 继续执行所用模型配置
     * @param sessionId 会话 ID
     * @param agentCode 智能体编码
     * @param runId 运行 ID
     * @param agentVersionId 智能体版本 ID
     * @param toolResult 工具执行结果
     * @return 审批通过后的继续执行结果。
     */
    private ContinuedOutput continueRunByModel(AgentVersion version,
                                              ModelConfig model,
                                              Long sessionId,
                                              String agentCode,
                                              String runId, 
                                              Long agentVersionId,
                                              String toolResult) {
        if (version == null) {
            throw new BusinessException("AgentVersion 不能为空");
        }
        if (model == null) {
            throw new BusinessException("未找到可用模型");
        }
        boolean enableTools = model.getToolEnabled() == null || Boolean.TRUE.equals(model.getToolEnabled());
        ChatClient chatClient = chatClientAssemblyService.buildChatClient(model, enableTools);

        String system = version.getSystemPromptSnapshot();
        if (!StringUtils.hasText(system)) {
            system = "";
        }
        String injected = buildToolResultInjection(toolResult);

        String user = "";
        String ragTagsJson = null;
        try {
            AgentRunContextSnapshot snap = loadSnapshotFromRunContext(runId);
            if (snap != null && StringUtils.hasText(snap.content)) {
                user = snap.content;
            }
            if (snap != null && StringUtils.hasText(snap.ragTagsJson)) {
                ragTagsJson = snap.ragTagsJson;
            }
        } catch (Exception e) {
            // 快照缺失不阻断兜底使用空输入
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

        ChatResponse resp = chatClient.prompt(prompt).call().chatResponse();
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

    /**
     * 将 RAG 文档拼接为模型可读上下文。
     *
     * @param docs 文档列表
     * @return 拼接后的文档上下文文本
     */
    private String formatRagDocuments(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (Document d : docs) {
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

    /**
     * 应用 RAG 覆盖配置。
     *
     * @param contract 协议结果
     * @param rag RAG 配置
     */
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
                actions = new ArrayList<>();
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
                new UserMessage("请将以下内容修复为符合要求的 JSON\n" + safe)
        );
        ChatResponse resp = client.prompt(prompt).call().chatResponse();
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

    /**
     * 获取Snapshot运行Context。
     *
     * @param runId 运行 ID
     * @return 运行上下文快照。
     */
    private AgentRunContextSnapshot loadSnapshotFromRunContext(String runId) {
        if (agentRunContextRepository == null || !StringUtils.hasText(runId)) {
            return null;
        }
        return agentRunContextRepository.findByRunId(runId)
                .map(ctx -> {
                    if (ctx == null || !StringUtils.hasText(ctx.getSnapshotJson())) {
                        return null;
                    }
                    try {
                        Map<String, Object> map = JsonMapUtils.readMap(objectMapper, ctx.getSnapshotJson());
                        String content = map == null ? null : String.valueOf(map.get("content"));
                        String ragTagsJson = map == null ? null : (map.get("ragTagsJson") == null ? null : String.valueOf(map.get("ragTagsJson")));
                        return new AgentRunContextSnapshot(content, ragTagsJson);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    /**
     * 解析模型版本。
     *
     * @param version 工作流版本
     * @return 解析后的模型配置
     */
    private ModelConfig resolveModelForVersion(AgentVersion version) {
        if (version == null) {
            return null;
        }
        List<ModelConfig> enabled = modelConfigService.queryEnabledModels(new EnabledQuery(true));
        if (enabled == null || enabled.isEmpty()) {
            throw new BusinessException("未配置可用模型");
        }
        return enabled.stream()
                .filter(m -> m != null && m.getId() != null)
                .sorted(Comparator.comparingLong(m -> m.getId() == null ? Long.MAX_VALUE : m.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未配置可用模型"));
    }

    private static final class AgentRunContextSnapshot {

        /**
         * 原始输入内容快照。
         */
        private final String content;

        /**
         * RAG 标签 JSON 快照。
         */
        private final String ragTagsJson;

        private AgentRunContextSnapshot(String content, String ragTagsJson) {
            this.content = content;
            this.ragTagsJson = ragTagsJson;
        }
    }

    private static final class ContinuedOutput {

        /**
         * 续跑后的平台协议结果。
         */
        private final PlatformContractV1 contract;

        /**
         * 输出修复尝试次数。
         */
        private final int repairAttempts;

        private ContinuedOutput(PlatformContractV1 contract, int repairAttempts) {
            this.contract = contract;
            this.repairAttempts = repairAttempts;
        }
    }
}
