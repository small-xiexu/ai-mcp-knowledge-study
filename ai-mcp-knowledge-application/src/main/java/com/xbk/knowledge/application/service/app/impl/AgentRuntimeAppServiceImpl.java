package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.WorkflowRuntimeAppService;
import com.xbk.knowledge.application.service.runtime.AgentEnhancerRuntimeService;
import com.xbk.knowledge.application.support.contract.PlatformContractV1OutputSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport.ResolvedRag;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.entity.AgentRun;
import com.xbk.knowledge.domain.agent.model.entity.AgentRunContext;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPlanningConfig;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentClientProfileStep;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentVersionRepository;
import com.xbk.knowledge.domain.client.adapter.repository.ClientProfileRepository;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowVersionRepository;
import com.xbk.knowledge.domain.llm.service.IModelConfigService;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.contract.PlatformStreamEvent;
import com.xbk.knowledge.types.exception.ApprovalRequiredException;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.MDC;

/**
 * Agent 运行入口应用服务实现。
 *
 * P0 策略
 * - 必须按 agentCode 找到 Agent
 * - 必须使用当前发布版本（Agent.current_published_version_id）
 * - 输出 Platform Contract v1（先不做结构化解析修复；P1 再加）
 * - 工具调用暂不启用（先保证主链路稳定；Iteration 3 再做 allowlist + toolKey）
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRuntimeAppServiceImpl implements AgentRuntimeAppService {

    /**
     * 规划审批类型编码。
     */
    private static final String PLAN_APPROVAL_TYPE = "PLAN_EXECUTE";

    /**
     * 规划审批工具键。
     */
    private static final String PLAN_APPROVAL_TOOL_KEY = "agent.plan.execute";

    /**
     * 规划就绪状态值。
     */
    private static final String PLANNING_STATE_READY = "PLANNING_READY";

    /**
     * 规划恢复状态值。
     */
    private static final String PLANNING_STATE_RESUMED = "PLANNING_RESUMED";

    /**
     * Agent 仓储。
     */
    private final AgentRepository agentRepository;

    /**
     * Agent 版本仓储。
     */
    private final AgentVersionRepository agentVersionRepository;

    /**
     * Agent 运行记录仓储。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * Agent 运行上下文仓储。
     */
    private final AgentRunContextRepository agentRunContextRepository;

    /**
     * 审批单仓储。
     */
    private final ApprovalRequestRepository approvalRequestRepository;

    /**
     * 模型配置领域服务。
     */
    private final IModelConfigService modelConfigService;

    /**
     * ChatClient 组装服务。
     */
    private final ChatClientAssemblyService chatClientAssemblyService;

    /**
     * Agent 增强器运行时服务。
     */
    private final AgentEnhancerRuntimeService agentEnhancerRuntimeService;

    /**
     * JSON 序列化/反序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 平台协议输出支持组件。
     */
    private final PlatformContractV1OutputSupport outputSupport;

    /**
     * RAG 治理支持组件。
     */
    private final AgentRagGovernanceSupport ragGovernanceSupport;

    /**
     * Workflow 运行时应用服务。
     */
    private final WorkflowRuntimeAppService workflowRuntimeAppService;

    /**
     * Workflow 版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * Workflow 仓储。
     */
    private final WorkflowRepository workflowRepository;

    /**
     * 客户端画像仓储。
     */
    private final ClientProfileRepository clientProfileRepository;

    /**
     * 执行 Agent 对话调用。
     * 
     * @param agentCode Agent 编码
     * @param sessionId 会话 ID
     * @param content 输入内容
     * @param ragTagsJson RAG 标签 JSON。
     * @return PlatformContractV1 数据。
     */
    @Override
    public PlatformContractV1 chat(String agentCode, Long sessionId, String content, String ragTagsJson) {
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(agentCode);
        AgentVersion version = loadPublishedVersion(agent);
        AgentPlanningConfig planningConfig = parsePlanningConfig(version);
        if (Boolean.TRUE.equals(planningConfig.getEnabled())) {
            return planAndMaybeExecute(
                    agentCode,
                    agent,
                    version,
                    sessionId,
                    content,
                    ragTagsJson,
                    start,
                    runId,
                    "CHAT_SYNC",
                    "HTTP",
                    null,
                    "system",
                    planningConfig
            );
        }
        List<AgentClientProfileStep> chainSteps = parseClientChainSteps(version);

        // Agent 绑定 Workflow直接走 WorkflowRuntime（并返回 steps 明细）
        if (version != null && version.getWorkflowVersionId() != null) {
            return chatByWorkflow(agentCode, agent, version, sessionId, content, start, runId);
        }
        if (!chainSteps.isEmpty()) {
            return chatByClientChain(
                    agentCode,
                    agent,
                    version,
                    chainSteps,
                    sessionId,
                    content,
                    ragTagsJson,
                    start,
                    runId,
                    "CHAT_SYNC",
                    "HTTP",
                    null,
                    "system"
            );
        }

        ModelConfig model = resolveModelForVersion(version);

        Long operatorId = null;
        String operatorType = operatorId == null ? "system" : "user";

        AgentRun run = AgentRun.builder()
                .runId(runId)
                .agentId(agent.getId())
                .agentCode(agentCode)
                .agentVersionId(version.getId())
                .runType("CHAT_SYNC")
                .triggerSource("HTTP")
                .operatorId(operatorId)
                .operatorType(operatorType)
                .sessionId(sessionId)
                .status("RUNNING")
                .modelIdUsed(model == null ? null : model.getId())
                .modelNameUsed(model == null ? null : model.getModelName())
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .toolCallCount(0)
                .toolDeniedCount(0)
                .repairAttempts(0)
                .startedAt(LocalDateTime.now())
                .build();
        agentRunRepository.insert(run);
        saveRunContextSnapshot(runId, agentCode, agent.getId(), version, model, sessionId, content, ragTagsJson);

        try {
            ResolvedRag rag = ragGovernanceSupport.resolve(version, ragTagsJson, content);
            if (rag != null && rag.requiredMiss()) {
                long costMs = System.currentTimeMillis() - start;
                AgentRun toUpdate = AgentRun.builder()
                        .runId(runId)
                        .status("SUCCESS")
                        .modelIdUsed(null)
                        .modelNameUsed(null)
                        .totalTokens(0)
                        .toolCallCount(null)
                        .toolDeniedCount(null)
                        .repairAttempts(0)
                        .costMs(costMs)
                        .endedAt(LocalDateTime.now())
                        .build();
                agentRunRepository.updateStatusAndMetrics(toUpdate);
                return buildRagRequiredNoHitContract(runId,  agentCode, version, costMs, rag);
            }

            ParsedOutput parsed = callOnce(runId, model, version, sessionId, content, rag);
            long costMs = System.currentTimeMillis() - start;

            AgentRun toUpdate = AgentRun.builder()
                    .runId(runId)
                    .status("SUCCESS")
                    .modelIdUsed(model == null ? null : model.getId())
                    .modelNameUsed(model == null ? null : model.getModelName())
                    .totalTokens(null)
                    .toolCallCount(null)
                    .toolDeniedCount(null)
                    .repairAttempts(parsed == null ? 0 : parsed.repairAttempts)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build();
            agentRunRepository.updateStatusAndMetrics(toUpdate);

            return buildSuccessContract(runId,  agentCode, version, model, parsed, costMs);
        } catch (ApprovalRequiredException e) {
            // 高风险工具触发审批run 进入待审批态，不结束 run（endedAt 置空）
            agentRunRepository.updateStatus(runId, "PENDING_APPROVAL", null, null);
            long costMs = System.currentTimeMillis() - start;
            return buildPendingApprovalContract(runId,  agentCode, version, model, costMs, e);
        } catch (OutputParseFailedException e) {
            long costMs = System.currentTimeMillis() - start;
            AgentRun toUpdate = AgentRun.builder()
                    .runId(runId)
                    .status("FAILED")
                    .errorMessage(truncate(e.getMessage(), 1000))
                    .repairAttempts(e.repairAttempts)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build();
            agentRunRepository.updateStatusAndMetrics(toUpdate);
            return buildFailedContract(runId,  agentCode, version, model, costMs, e.getMessage(), e.repairAttempts);
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            String msg = e.getMessage();
            AgentRun toUpdate = AgentRun.builder()
                    .runId(runId)
                    .status("FAILED")
                    .errorMessage(truncate(msg, 1000))
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build();
            agentRunRepository.updateStatusAndMetrics(toUpdate);
            return buildFailedContract(runId,  agentCode, version, model, costMs, msg, 0);
        }
    }

    /**
     * 按工作流模式执行对话请求。
     * 
     * @param agentCode 智能体编码。
     * @param agent 智能体。
     * @param version 工作流版本。
     * @param sessionId 会话 ID。
     * @param content 用户输入内容。
     * @param start 开始时间。
     * @param runId 运行ID。
     * @return 平台协议结果。
     */
    private PlatformContractV1 chatByWorkflow(String agentCode,
                                              Agent agent,
                                              AgentVersion version,
                                              Long sessionId,
                                              String content,
                                              long start,
                                              String runId) {
        if (workflowRuntimeAppService == null || workflowVersionRepository == null || workflowRepository == null) {
            throw new BusinessException("WorkflowRuntime 依赖未注入，无法按 workflowVersionId 执行");
        }
        Long wfVersionId = version.getWorkflowVersionId();
        WorkflowVersion wfVersion = workflowVersionRepository.findById(
                        WorkflowVersionIdQuery.builder().id(wfVersionId).build())
                .orElseThrow(() -> new NotFoundException("绑定的 WorkflowVersion 不存在，id=" + wfVersionId));
        Workflow wf = workflowRepository.findById(new IdQuery(wfVersion.getWorkflowId()))
                .orElseThrow(() -> new NotFoundException("绑定的 Workflow 不存在，id=" + wfVersion.getWorkflowId()));

        Long operatorId = null;
        String operatorType = operatorId == null ? "system" : "user";

        // 仍写入 agent_run，便于审计；工具审批优先按 workflow 归属生成审批单（见 ToolCallbackProvider 的逻辑修正）
        AgentRun run = AgentRun.builder()
                .runId(runId)
                .agentId(agent == null ? null : agent.getId())
                .agentCode(agentCode)
                .agentVersionId(version.getId())
                .runType("CHAT_SYNC_WORKFLOW")
                .triggerSource("HTTP")
                .operatorId(operatorId)
                .operatorType(operatorType)
                .sessionId(sessionId)
                .status("RUNNING")
                .modelIdUsed(null)
                .modelNameUsed(null)
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .toolCallCount(0)
                .toolDeniedCount(0)
                .repairAttempts(0)
                .startedAt(LocalDateTime.now())
                .build();
        agentRunRepository.insert(run);

        String prev = MDC.get(TraceIdUtils.TRACE_ID_KEY);
        MDC.put(TraceIdUtils.TRACE_ID_KEY, runId);
        try {
            PlatformContractV1 contract = workflowRuntimeAppService.run(wf.getWorkflowCode(),
                    sessionId,
                    content,
                    null,
                    wfVersionId
            );
            long costMs = System.currentTimeMillis() - start;

            // agent_run 状态同步（workflow_run 由 workflowRuntime 自行维护）
            try {
                agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                        .runId(runId)
                        .status(contract == null ? "FAILED" : contract.getStatus())
                        .costMs(costMs)
                        .endedAt(LocalDateTime.now())
                        .build());
            } catch (Exception ignore) {
            }

            // 补齐 agent 元信息，确保调用方（Agent 调用入口）仍能识别 agent 归属
            if (contract != null) {
                PlatformContractV1.Meta meta = contract.getMeta();
                if (meta == null) {
                    meta = new PlatformContractV1.Meta();
                    contract.setMeta(meta);
                }
                meta.setRunId(runId);
                meta.setAgentCode(agentCode);
                meta.setAgentVersionId(version.getId());
                meta.setAgentVersionNo(version.getVersionNo());
            }
            return contract;
        } finally {
            if (prev == null) {
                MDC.remove(TraceIdUtils.TRACE_ID_KEY);
            } else {
                MDC.put(TraceIdUtils.TRACE_ID_KEY, prev);
            }
        }
    }

    private PlatformContractV1 chatByClientChain(String agentCode,
                                                 Agent agent,
                                                 AgentVersion version,
                                                 List<AgentClientProfileStep> chainSteps,
                                                 Long sessionId,
                                                 String content,
                                                 String ragTagsJson,
                                                 long start,
                                                 String runId,
                                                 String runType,
                                                 String triggerSource,
                                                 Long operatorId,
                                                 String operatorType) {
        AgentRun run = AgentRun.builder()
                .runId(runId)
                .agentId(agent == null ? null : agent.getId())
                .agentCode(agentCode)
                .agentVersionId(version == null ? null : version.getId())
                .runType(runType)
                .triggerSource(triggerSource)
                .operatorId(operatorId)
                .operatorType(operatorType)
                .sessionId(sessionId)
                .status("RUNNING")
                .modelIdUsed(null)
                .modelNameUsed(null)
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .toolCallCount(0)
                .toolDeniedCount(0)
                .repairAttempts(0)
                .startedAt(LocalDateTime.now())
                .build();
        agentRunRepository.insert(run);
        saveRunContextSnapshot(runId, agentCode, agent == null ? null : agent.getId(), version, null, sessionId, content, ragTagsJson);

        List<PlatformContractV1.StepTrace> stepTraces = new ArrayList<>();
        int totalRepairAttempts = 0;
        ModelConfig lastModel = null;
        ParsedOutput lastParsed = null;
        String stepInput = content == null ? "" : content;
        ResolvedRag rag = null;
        try {
            rag = ragGovernanceSupport.resolve(version, ragTagsJson, content);
            if (rag != null && rag.requiredMiss()) {
                long costMs = System.currentTimeMillis() - start;
                agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                        .runId(runId)
                        .status("SUCCESS")
                        .modelIdUsed(null)
                        .modelNameUsed(null)
                        .totalTokens(0)
                        .toolCallCount(null)
                        .toolDeniedCount(null)
                        .repairAttempts(0)
                        .costMs(costMs)
                        .endedAt(LocalDateTime.now())
                        .build());
                return buildRagRequiredNoHitContract(runId, agentCode, version, costMs, rag);
            }

            for (int i = 0; i < chainSteps.size(); i++) {
                AgentClientProfileStep step = chainSteps.get(i);
                long stepStart = System.currentTimeMillis();
                String stepName = normalizeChainStepName(step, i + 1);
                ModelConfig stepModel = resolveEnabledModelById(step.getModelId(), stepName);
                boolean enableTools = resolveStepEnableTools(step, stepModel);
                Set<String> allowedToolKeys = resolveAllowedToolKeys(step, version);
                String systemPromptOverride = StringUtils.hasText(step.getSystemPrompt()) ? step.getSystemPrompt() : null;
                ResolvedRag stepRag = i == 0 ? rag : null;
                try {
                    ParsedOutput parsed = callOnce(
                            runId,
                            stepModel,
                            version,
                            sessionId,
                            stepInput,
                            stepRag,
                            systemPromptOverride,
                            allowedToolKeys,
                            enableTools
                    );
                    long stepCost = System.currentTimeMillis() - stepStart;
                    totalRepairAttempts += parsed == null ? 0 : parsed.repairAttempts;
                    stepTraces.add(buildChainStepTrace(step, i + 1, "SUCCESS", stepCost, parsed == null ? null : parsed.contract, null, null));
                    lastModel = stepModel;
                    lastParsed = parsed;
                    if (parsed != null && parsed.contract != null && parsed.contract.getAnswer() != null) {
                        stepInput = parsed.contract.getAnswer();
                    } else {
                        stepInput = "";
                    }
                } catch (ApprovalRequiredException approval) {
                    long stepCost = System.currentTimeMillis() - stepStart;
                    stepTraces.add(buildChainStepTrace(step, i + 1, "PENDING_APPROVAL", stepCost, null, approval.getApprovalRequestId(), approval.getMessage()));
                    agentRunRepository.updateStatus(runId, "PENDING_APPROVAL", null, null);
                    long costMs = System.currentTimeMillis() - start;
                    return buildPendingApprovalContractWithSteps(
                            runId,
                            agentCode,
                            version,
                            stepModel,
                            costMs,
                            approval,
                            stepTraces
                    );
                } catch (OutputParseFailedException parseFailed) {
                    long stepCost = System.currentTimeMillis() - stepStart;
                    int currentRepair = totalRepairAttempts + parseFailed.repairAttempts;
                    stepTraces.add(buildChainStepTrace(step, i + 1, "FAILED", stepCost, null, null, parseFailed.getMessage()));
                    long costMs = System.currentTimeMillis() - start;
                    agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                            .runId(runId)
                            .status("FAILED")
                            .errorMessage(truncate(parseFailed.getMessage(), 1000))
                            .repairAttempts(currentRepair)
                            .costMs(costMs)
                            .endedAt(LocalDateTime.now())
                            .build());
                    return buildFailedContractWithSteps(
                            runId,
                            agentCode,
                            version,
                            stepModel,
                            costMs,
                            parseFailed.getMessage(),
                            currentRepair,
                            stepTraces
                    );
                } catch (Exception e) {
                    long stepCost = System.currentTimeMillis() - stepStart;
                    String message = e.getMessage();
                    stepTraces.add(buildChainStepTrace(step, i + 1, "FAILED", stepCost, null, null, message));
                    long costMs = System.currentTimeMillis() - start;
                    agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                            .runId(runId)
                            .status("FAILED")
                            .errorMessage(truncate(message, 1000))
                            .repairAttempts(totalRepairAttempts)
                            .costMs(costMs)
                            .endedAt(LocalDateTime.now())
                            .build());
                    return buildFailedContractWithSteps(
                            runId,
                            agentCode,
                            version,
                            stepModel,
                            costMs,
                            message,
                            totalRepairAttempts,
                            stepTraces
                    );
                }
            }

            if (lastParsed == null) {
                throw new BusinessException("Agent clientChain 执行失败未产出有效结果");
            }
            long costMs = System.currentTimeMillis() - start;
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .status("SUCCESS")
                    .modelIdUsed(lastModel == null ? null : lastModel.getId())
                    .modelNameUsed(lastModel == null ? null : lastModel.getModelName())
                    .totalTokens(null)
                    .toolCallCount(null)
                    .toolDeniedCount(null)
                    .repairAttempts(totalRepairAttempts)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build());
            return buildSuccessContractWithSteps(
                    runId,
                    agentCode,
                    version,
                    lastModel,
                    lastParsed,
                    costMs,
                    totalRepairAttempts,
                    stepTraces
            );
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            String message = e.getMessage();
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .status("FAILED")
                    .errorMessage(truncate(message, 1000))
                    .repairAttempts(totalRepairAttempts)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build());
            return buildFailedContractWithSteps(
                    runId,
                    agentCode,
                    version,
                    lastModel,
                    costMs,
                    message,
                    totalRepairAttempts,
                    stepTraces
            );
        }
    }

    /**
     * 执行 Agent 流式调用。
     * 
     * @param agentCode Agent 编码
     * @param sessionId 会话 ID
     * @param content 输入内容
     * @param ragTagsJson RAG 标签 JSON。
     * @return Flux<PlatformStreamEvent> 数据。
     */
    @Override
    public Flux<PlatformStreamEvent> stream(String agentCode, Long sessionId, String content, String ragTagsJson) {
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(agentCode);
        AgentVersion version = loadPublishedVersion(agent);
        AgentPlanningConfig planningConfig = parsePlanningConfig(version);
        if (version != null && (Boolean.TRUE.equals(planningConfig.getEnabled())
                || version.getWorkflowVersionId() != null
                || hasClientChain(version))) {
            PlatformContractV1 contract = chat(agentCode, sessionId, content, ragTagsJson);
            if (contract == null || contract.getSteps() == null || contract.getSteps().isEmpty()) {
                return Flux.just(PlatformStreamEvent.builder().name("final").data(contract).build());
            }
            return Flux.fromIterable(contract.getSteps())
                    .map(step -> PlatformStreamEvent.builder().name("step").data(step).build())
                    .concatWith(Flux.just(PlatformStreamEvent.builder().name("final").data(contract).build()));
        }
        ModelConfig model = resolveModelForVersion(version);

        Long operatorId = null;
        String operatorType = operatorId == null ? "system" : "user";

        AgentRun run = AgentRun.builder()
                .runId(runId)
                .agentId(agent.getId())
                .agentCode(agentCode)
                .agentVersionId(version.getId())
                .runType("CHAT_STREAM")
                .triggerSource("HTTP")
                .operatorId(operatorId)
                .operatorType(operatorType)
                .sessionId(sessionId)
                .status("RUNNING")
                .modelIdUsed(model == null ? null : model.getId())
                .modelNameUsed(model == null ? null : model.getModelName())
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .toolCallCount(0)
                .toolDeniedCount(0)
                .repairAttempts(0)
                .startedAt(LocalDateTime.now())
                .build();
        agentRunRepository.insert(run);
        saveRunContextSnapshot(runId, agentCode, agent.getId(), version, model, sessionId, content, ragTagsJson);

        AtomicInteger totalTokens = new AtomicInteger(0);

        ResolvedRag rag = ragGovernanceSupport.resolve(version, ragTagsJson, content);
        if (rag != null && rag.requiredMiss()) {
            long costMs = System.currentTimeMillis() - start;
            AgentRun toUpdate = AgentRun.builder()
                    .runId(runId)
                    .status("SUCCESS")
                    .modelIdUsed(null)
                    .modelNameUsed(null)
                    .totalTokens(0)
                    .toolCallCount(null)
                    .toolDeniedCount(null)
                    .repairAttempts(0)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build();
            agentRunRepository.updateStatusAndMetrics(toUpdate);
            PlatformContractV1 finalContract = buildRagRequiredNoHitContract(runId,  agentCode, version, costMs, rag);
            return Flux.just(PlatformStreamEvent.builder().name("final").data(finalContract).build());
        }

        Prompt prompt = buildPromptWithContract(version, content, rag);
        StringBuilder answerBuffer = new StringBuilder();
        Set<String> allowedToolKeys = parseAllowedToolKeys(version == null ? null : version.getAllowedToolKeysJson());

        ChatClient chatClient = buildChatClient(runId, model, version, sessionId);
        Long modelId = model == null ? null : model.getId();
        Long agentVersionId = version == null ? null : version.getId();

        return Flux.using(
                        () -> {
                            if (modelId != null) {
                                GatewayToolBindingContextHolder.set(modelId, sessionId, agentVersionId, runId, allowedToolKeys);
                            }
                            return Boolean.TRUE;
                        },
                        ignored -> chatClient.prompt(prompt).stream().chatResponse(),
                        ignored -> GatewayToolBindingContextHolder.clear()
                )
                .doOnNext(resp -> {
                    String delta = extractDelta(resp);
                    if (delta != null && !delta.isEmpty()) {
                        answerBuffer.append(delta);
                    }
                    Usage usage = resp != null && resp.getMetadata() != null ? resp.getMetadata().getUsage() : null;
                    if (usage != null && usage.getTotalTokens() != null) {
                        totalTokens.set(usage.getTotalTokens());
                    }
                })
                .map(resp -> {
                    String delta = extractDelta(resp);
                    return PlatformStreamEvent.builder()
                            .name("delta")
                            .data(delta == null ? "" : delta)
                            .build();
                })
                .doOnError(e -> {
                    if (e instanceof ApprovalRequiredException) {
                        agentRunRepository.updateStatus(runId, "PENDING_APPROVAL", null, null);
                        return;
                    }
                    long costMs = System.currentTimeMillis() - start;
                    AgentRun toUpdate = AgentRun.builder()
                            .runId(runId)
                            .status("FAILED")
                            .errorMessage(truncate(e.getMessage(), 1000))
                            .costMs(costMs)
                            .endedAt(LocalDateTime.now())
                            .build();
                    agentRunRepository.updateStatusAndMetrics(toUpdate);
                })
                .concatWith(Flux.defer(() -> {
                    long costMs = System.currentTimeMillis() - start;
                    try {
                        ParsedOutput parsed = parseOrRepairFromStream(model, version, sessionId, answerBuffer.toString());
                        applyRagOverrides(parsed, rag);
                        AgentRun toUpdate = AgentRun.builder()
                                .runId(runId)
                                .status("SUCCESS")
                                .modelIdUsed(model == null ? null : model.getId())
                                .modelNameUsed(model == null ? null : model.getModelName())
                                .totalTokens(totalTokens.get())
                                .toolCallCount(null)
                                .toolDeniedCount(null)
                                .repairAttempts(parsed == null ? 0 : parsed.repairAttempts)
                                .costMs(costMs)
                                .endedAt(LocalDateTime.now())
                                .build();
                        agentRunRepository.updateStatusAndMetrics(toUpdate);
                        PlatformContractV1 finalContract = buildSuccessContract(runId,  agentCode, version, model, parsed, costMs);
                        return Flux.just(PlatformStreamEvent.builder().name("final").data(finalContract).build());
                    } catch (OutputParseFailedException e) {
                        AgentRun toUpdate = AgentRun.builder()
                                .runId(runId)
                                .status("FAILED")
                                .errorMessage(truncate(e.getMessage(), 1000))
                                .repairAttempts(e.repairAttempts)
                                .costMs(costMs)
                                .endedAt(LocalDateTime.now())
                                .build();
                        agentRunRepository.updateStatusAndMetrics(toUpdate);
                        PlatformContractV1 failed = buildFailedContract(runId,  agentCode, version, model, costMs, e.getMessage(), e.repairAttempts);
                        return Flux.just(PlatformStreamEvent.builder().name("final").data(failed).build());
                    }
                }))
                .onErrorResume(e -> {
                    long costMs = System.currentTimeMillis() - start;
                    if (e instanceof ApprovalRequiredException approval) {
                        PlatformContractV1 pending = buildPendingApprovalContract(
                                runId, 
                                agentCode,
                                version,
                                model,
                                costMs,
                                approval
                        );
                        return Flux.just(PlatformStreamEvent.builder()
                                .name("final")
                                .data(pending)
                                .build());
                    }
                    PlatformContractV1 failed = buildFailedContract(
                            runId, 
                            agentCode,
                            version,
                            model,
                            costMs,
                            e.getMessage(),
                            0
                    );
                    return Flux.just(PlatformStreamEvent.builder()
                            .name("final")
                            .data(failed)
                            .build());
                });
    }

    /**
     * 执行业务调用。
     * 
     * @param agentCode Agent 编码
     * @param sessionId 会话 ID
     * @param content 输入内容
     * @param ragTagsJson RAG 标签 JSON。
     * @return PlatformContractV1 数据。
     */
    @Override
    public PlatformContractV1 invoke(String agentCode, Long sessionId, String content, String ragTagsJson) {
        // 内部触发同样走 current published version
        return chat(agentCode, sessionId, content, ragTagsJson);
    }

    /**
     * 审批通过后继续执行 Planning 任务。
     * 
     * @param runId 运行ID
     * @param approvalRequestId 审批单ID
     * @return 平台标准结构化结果
     */
    @Override
    public PlatformContractV1 resumePlannedRun(String runId, Long approvalRequestId) {
        if (!StringUtils.hasText(runId)) {
            throw new IllegalArgumentException("runId 不能为空");
        }
        AgentRun run = agentRunRepository.findByRunId(runId)
                .orElseThrow(() -> new NotFoundException("run 不存在，runId=" + runId));
        AgentVersion version = agentVersionRepository.findById(new AgentVersionIdQuery(run.getAgentVersionId()))
                .orElseThrow(() -> new NotFoundException("AgentVersion 不存在，id=" + run.getAgentVersionId()));
        Agent agent = agentRepository.findByCode(new AgentCodeQuery(run.getAgentCode()))
                .orElseThrow(() -> new NotFoundException("Agent 不存在，agentCode=" + run.getAgentCode()));
        AgentRunContext runContext = agentRunContextRepository.findByRunId(runId)
                .orElseThrow(() -> new NotFoundException("未找到 planning 运行上下文，runId=" + runId));

        Map<String, Object> snapshot = readJsonMap(runContext.getSnapshotJson());
        AgentExecutionPlan plan = readPlanFromSnapshot(snapshot);
        String content = asString(snapshot.get("content"));
        String ragTagsJson = asString(snapshot.get("ragTagsJson"));
        Long sessionId = run.getSessionId();
        if (sessionId == null) {
            sessionId = asLong(snapshot.get("sessionId"));
        }

        agentRunRepository.updateStatus(runId, "RUNNING", null, null);
        long start = System.currentTimeMillis();
        PlatformContractV1 result = executePlannedRun(
                runId,
                agent.getAgentCode(),
                agent,
                version,
                sessionId,
                content,
                ragTagsJson,
                start,
                plan
        );
        try {
            Map<String, Object> extras = new HashMap<>();
            extras.put("planningState", PLANNING_STATE_RESUMED);
            extras.put("planApprovalRequestId", approvalRequestId);
            extras.put("resumeAt", LocalDateTime.now().toString());
            saveRunContextSnapshot(
                    runId,
                    agent.getAgentCode(),
                    agent.getId(),
                    version,
                    null,
                    sessionId,
                    content,
                    ragTagsJson,
                    extras
            );
            agentRunContextRepository.updateStatus(runId, "RESUMED");
        } catch (Exception ignore) {
        }
        return result;
    }

    /**
     * 执行业务流程。
     * 
     * @param agentCode Agent 编码
     * @param content 输入内容
     * @param ragTagsJson RAG 标签 JSON。
     * @return PlatformContractV1 数据。
     */
    @Override
    public PlatformContractV1 runJob(String agentCode, String content, String ragTagsJson) {
        // XXL 调度触发与 chat 的核心逻辑一致，但 runType/triggerSource 固定，并且操作者为 system
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(agentCode);
        AgentVersion version = loadPublishedVersion(agent);
        AgentPlanningConfig planningConfig = parsePlanningConfig(version);
        if (Boolean.TRUE.equals(planningConfig.getEnabled())) {
            return planAndMaybeExecute(
                    agentCode,
                    agent,
                    version,
                    null,
                    content,
                    ragTagsJson,
                    start,
                    runId,
                    "XXL_JOB",
                    "XXL",
                    null,
                    "system",
                    planningConfig
            );
        }
        List<AgentClientProfileStep> chainSteps = parseClientChainSteps(version);
        if (!chainSteps.isEmpty()) {
            return chatByClientChain(
                    agentCode,
                    agent,
                    version,
                    chainSteps,
                    null,
                    content,
                    ragTagsJson,
                    start,
                    runId,
                    "XXL_JOB",
                    "XXL",
                    null,
                    "system"
            );
        }
        ModelConfig model = resolveModelForVersion(version);

        AgentRun run = AgentRun.builder()
                .runId(runId)
                .agentId(agent.getId())
                .agentCode(agentCode)
                .agentVersionId(version.getId())
                .runType("XXL_JOB")
                .triggerSource("XXL")
                .operatorId(null)
                .operatorType("system")
                .sessionId(null)
                .status("RUNNING")
                .modelIdUsed(model == null ? null : model.getId())
                .modelNameUsed(model == null ? null : model.getModelName())
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .toolCallCount(0)
                .toolDeniedCount(0)
                .repairAttempts(0)
                .startedAt(LocalDateTime.now())
                .build();
        agentRunRepository.insert(run);
        saveRunContextSnapshot(runId, agentCode, agent.getId(), version, model, null, content, ragTagsJson);

        try {
            ResolvedRag rag = ragGovernanceSupport.resolve(version, ragTagsJson, content);
            if (rag != null && rag.requiredMiss()) {
                long costMs = System.currentTimeMillis() - start;
                AgentRun toUpdate = AgentRun.builder()
                        .runId(runId)
                        .status("SUCCESS")
                        .modelIdUsed(null)
                        .modelNameUsed(null)
                        .totalTokens(0)
                        .toolCallCount(null)
                        .toolDeniedCount(null)
                        .repairAttempts(0)
                        .costMs(costMs)
                        .endedAt(LocalDateTime.now())
                        .build();
                agentRunRepository.updateStatusAndMetrics(toUpdate);
                return buildRagRequiredNoHitContract(runId,  agentCode, version, costMs, rag);
            }

            ParsedOutput parsed = callOnce(runId, model, version, null, content, rag);
            long costMs = System.currentTimeMillis() - start;
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .status("SUCCESS")
                    .modelIdUsed(model == null ? null : model.getId())
                    .modelNameUsed(model == null ? null : model.getModelName())
                    .totalTokens(null)
                    .toolCallCount(null)
                    .toolDeniedCount(null)
                    .repairAttempts(parsed == null ? 0 : parsed.repairAttempts)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build());
            return buildSuccessContract(runId,  agentCode, version, model, parsed, costMs);
        } catch (ApprovalRequiredException e) {
            // XXL 模式按约定Job 不失败；run 进入待审批态
            agentRunRepository.updateStatus(runId, "PENDING_APPROVAL", null, null);
            long costMs = System.currentTimeMillis() - start;
            return buildPendingApprovalContract(runId,  agentCode, version, model, costMs, e);
        } catch (OutputParseFailedException e) {
            long costMs = System.currentTimeMillis() - start;
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .status("FAILED")
                    .errorMessage(truncate(e.getMessage(), 1000))
                    .repairAttempts(e.repairAttempts)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build());
            return buildFailedContract(runId,  agentCode, version, model, costMs, e.getMessage(), e.repairAttempts);
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            String msg = e.getMessage();
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .status("FAILED")
                    .errorMessage(truncate(msg, 1000))
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build());
            return buildFailedContract(runId,  agentCode, version, model, costMs, msg, 0);
        }
    }

    /**
     * 生成执行计划并按需继续执行。
     * 
     * @param agentCode 智能体编码。
     * @param agent 智能体。
     * @param version 工作流版本。
     * @param sessionId 会话 ID。
     * @param content 用户输入内容。
     * @param ragTagsJson RAG标签JSON。
     * @param start 开始时间。
     * @param runId 运行ID。
     * @param runType 运行类型。
     * @param triggerSource 触发来源。
     * @param operatorId 操作人ID。
     * @param operatorType 操作者类型。
     * @param planningConfig 规划配置。
     * @return 平台协议结果。
     */
    private PlatformContractV1 planAndMaybeExecute(String agentCode,
                                                   Agent agent,
                                                   AgentVersion version,
                                                   Long sessionId,
                                                   String content,
                                                   String ragTagsJson,
                                                   long start,
                                                   String runId,
                                                   String runType,
                                                   String triggerSource,
                                                   Long operatorId,
                                                   String operatorType,
                                                   AgentPlanningConfig planningConfig) {
        ModelConfig plannerModel = resolvePlannerModel(planningConfig);
        AgentRun run = AgentRun.builder()
                .runId(runId)
                .agentId(agent == null ? null : agent.getId())
                .agentCode(agentCode)
                .agentVersionId(version == null ? null : version.getId())
                .runType(runType)
                .triggerSource(triggerSource)
                .operatorId(operatorId)
                .operatorType(operatorType)
                .sessionId(sessionId)
                .status("RUNNING")
                .modelIdUsed(plannerModel == null ? null : plannerModel.getId())
                .modelNameUsed(plannerModel == null ? null : plannerModel.getModelName())
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .toolCallCount(0)
                .toolDeniedCount(0)
                .repairAttempts(0)
                .startedAt(LocalDateTime.now())
                .build();
        agentRunRepository.insert(run);

        AgentExecutionPlan plan = generateExecutionPlan(runId, plannerModel, version, content, planningConfig);
        Map<String, Object> extras = new HashMap<>();
        extras.put("planningState", PLANNING_STATE_READY);
        extras.put("plan", plan);
        saveRunContextSnapshot(runId, agentCode, agent == null ? null : agent.getId(), version, plannerModel, sessionId, content, ragTagsJson, extras);

        if (planningConfig == null || !Boolean.TRUE.equals(planningConfig.getRequireHumanConfirm())) {
            return executePlannedRun(runId, agentCode, agent, version, sessionId, content, ragTagsJson, start, plan);
        }

        ApprovalRequest approvalRequest = createPlanApproval(run, plan, planningConfig);
        String riskLevel = approvalRequest == null ? "HIGH" : approvalRequest.getRiskLevel();
        ApprovalRequiredException pendingApproval = new ApprovalRequiredException(
                approvalRequest == null ? null : approvalRequest.getId(),
                PLAN_APPROVAL_TOOL_KEY,
                riskLevel,
                "Planning 计划待人工确认后执行"
        );
        agentRunRepository.updateStatus(runId, "PENDING_APPROVAL", null, null);
        extras.put("planApprovalRequestId", approvalRequest == null ? null : approvalRequest.getId());
        saveRunContextSnapshot(runId, agentCode, agent == null ? null : agent.getId(), version, plannerModel, sessionId, content, ragTagsJson, extras);

        long costMs = System.currentTimeMillis() - start;
        List<PlatformContractV1.StepTrace> plannedSteps = buildPlannedStepTraces(plan, approvalRequest == null ? null : approvalRequest.getId());
        return buildPendingApprovalContractWithSteps(
                runId,
                agentCode,
                version,
                plannerModel,
                costMs,
                pendingApproval,
                plannedSteps
        );
    }

    /**
     * 执行主流程并返回协议结果。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param agent 智能体。
     * @param version 工作流版本。
     * @param sessionId 会话 ID。
     * @param content 用户输入内容。
     * @param ragTagsJson RAG标签JSON。
     * @param start 开始时间。
     * @param plan 执行计划。
     * @return 平台协议结果。
     */
    private PlatformContractV1 executePlannedRun(String runId,
                                                 String agentCode,
                                                 Agent agent,
                                                 AgentVersion version,
                                                 Long sessionId,
                                                 String content,
                                                 String ragTagsJson,
                                                 long start,
                                                 AgentExecutionPlan plan) {
        if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
            throw new BusinessException("Planning 计划为空，无法执行");
        }
        List<PlatformContractV1.StepTrace> stepTraces = new ArrayList<>();
        ModelConfig lastModel = null;
        ParsedOutput lastParsed = null;
        int totalRepairAttempts = 0;
        String stepInput = content == null ? "" : content;
        ResolvedRag rag = ragGovernanceSupport.resolve(version, ragTagsJson, content);
        if (rag != null && rag.requiredMiss()) {
            long costMs = System.currentTimeMillis() - start;
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .status("SUCCESS")
                    .modelIdUsed(null)
                    .modelNameUsed(null)
                    .totalTokens(0)
                    .toolCallCount(null)
                    .toolDeniedCount(null)
                    .repairAttempts(0)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build());
            return buildRagRequiredNoHitContract(runId, agentCode, version, costMs, rag);
        }

        for (int i = 0; i < plan.getSteps().size(); i++) {
            PlanStep step = plan.getSteps().get(i);
            long stepStart = System.currentTimeMillis();
            ModelConfig stepModel = resolvePlanningStepModel(step);
            Set<String> allowedToolKeys = parseAllowedToolKeys(step == null ? null : step.getAllowedToolKeys());
            boolean enableTools = step != null && Boolean.TRUE.equals(step.getEnableTools());
            String userInput = buildPlanningStepInput(plan, stepInput, step, i + 1);
            try {
                ParsedOutput parsed = callOnce(
                        runId,
                        stepModel,
                        version,
                        sessionId,
                        userInput,
                        i == 0 ? rag : null,
                        step == null ? null : step.getInstruction(),
                        allowedToolKeys,
                        enableTools
                );
                long stepCost = System.currentTimeMillis() - stepStart;
                totalRepairAttempts += parsed == null ? 0 : parsed.repairAttempts;
                stepTraces.add(buildPlanningExecutionStepTrace(step, i + 1, "SUCCESS", stepCost, parsed == null ? null : parsed.contract, null, null));
                lastModel = stepModel;
                lastParsed = parsed;
                stepInput = parsed == null || parsed.contract == null ? "" : parsed.contract.getAnswer();
            } catch (ApprovalRequiredException approval) {
                long stepCost = System.currentTimeMillis() - stepStart;
                stepTraces.add(buildPlanningExecutionStepTrace(step, i + 1, "PENDING_APPROVAL", stepCost, null, approval.getApprovalRequestId(), approval.getMessage()));
                long costMs = System.currentTimeMillis() - start;
                agentRunRepository.updateStatus(runId, "PENDING_APPROVAL", null, null);
                return buildPendingApprovalContractWithSteps(runId, agentCode, version, stepModel, costMs, approval, stepTraces);
            } catch (OutputParseFailedException parseFailed) {
                long stepCost = System.currentTimeMillis() - stepStart;
                int currentRepair = totalRepairAttempts + parseFailed.repairAttempts;
                stepTraces.add(buildPlanningExecutionStepTrace(step, i + 1, "FAILED", stepCost, null, null, parseFailed.getMessage()));
                long costMs = System.currentTimeMillis() - start;
                agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                        .runId(runId)
                        .status("FAILED")
                        .errorMessage(truncate(parseFailed.getMessage(), 1000))
                        .repairAttempts(currentRepair)
                        .costMs(costMs)
                        .endedAt(LocalDateTime.now())
                        .build());
                return buildFailedContractWithSteps(runId, agentCode, version, stepModel, costMs, parseFailed.getMessage(), currentRepair, stepTraces);
            } catch (Exception e) {
                long stepCost = System.currentTimeMillis() - stepStart;
                String message = e.getMessage();
                stepTraces.add(buildPlanningExecutionStepTrace(step, i + 1, "FAILED", stepCost, null, null, message));
                long costMs = System.currentTimeMillis() - start;
                agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                        .runId(runId)
                        .status("FAILED")
                        .errorMessage(truncate(message, 1000))
                        .repairAttempts(totalRepairAttempts)
                        .costMs(costMs)
                        .endedAt(LocalDateTime.now())
                        .build());
                return buildFailedContractWithSteps(runId, agentCode, version, stepModel, costMs, message, totalRepairAttempts, stepTraces);
            }
        }

        if (lastParsed == null) {
            throw new BusinessException("Planning 执行失败未产出有效结果");
        }
        long costMs = System.currentTimeMillis() - start;
        agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                .runId(runId)
                .status("SUCCESS")
                .modelIdUsed(lastModel == null ? null : lastModel.getId())
                .modelNameUsed(lastModel == null ? null : lastModel.getModelName())
                .totalTokens(null)
                .toolCallCount(null)
                .toolDeniedCount(null)
                .repairAttempts(totalRepairAttempts)
                .costMs(costMs)
                .endedAt(LocalDateTime.now())
                .build());
        return buildSuccessContractWithSteps(runId, agentCode, version, lastModel, lastParsed, costMs, totalRepairAttempts, stepTraces);
    }

    /**
     * 生成多步骤执行计划。
     * 
     * @param runId 运行ID。
     * @param plannerModel 规划模型。
     * @param version 工作流版本。
     * @param content 用户输入内容。
     * @param planningConfig 规划配置。
     * @return 执行计划。
     */
    private AgentExecutionPlan generateExecutionPlan(String runId,
                                                     ModelConfig plannerModel,
                                                     AgentVersion version,
                                                     String content,
                                                     AgentPlanningConfig planningConfig) {
        String plannerSystem = """
                你是企业级 Agent 规划器。你必须把任务拆成可执行步骤，并严格输出 JSON。
                约束
                1) 只输出 JSON，不输出 markdown 或解释
                2) steps 至少 1 步，最多 maxPlanSteps 步
                3) 风险等级只能是 LOW/MEDIUM/HIGH
                4) 当步骤涉及外部写操作时，riskLevel 至少为 MEDIUM
                JSON 结构
                {
                  "goal": "任务目标",
                  "summary": "计划摘要",
                  "steps": [
                    {
                      "stepNo": 1,
                      "stepName": "步骤名",
                      "instruction": "该步骤执行指令（给模型）",
                      "modelId": 1,
                      "enableTools": false,
                      "allowedToolKeys": ["tool.a","tool.b"],
                      "riskLevel": "LOW",
                      "expectedOutput": "该步骤期望产物"
                    }
                  ]
                }
                """;
        String baseSystem = version == null ? null : version.getSystemPromptSnapshot();
        Prompt prompt = new Prompt(
                new SystemMessage(StringUtils.hasText(baseSystem) ? baseSystem : ""),
                new SystemMessage(plannerSystem),
                new UserMessage("maxPlanSteps=" + safeMaxPlanSteps(planningConfig) + "\n任务输入:\n" + (content == null ? "" : content))
        );
        ChatClient plannerClient = buildChatClient(runId, plannerModel, version, null, false);
        ChatResponse response = plannerClient.prompt(prompt).call().chatResponse();
        String raw = extractText(response);
        AgentExecutionPlan plan = parseExecutionPlan(raw, content, planningConfig);
        if (plan.getSteps().isEmpty()) {
            throw new BusinessException("Planner 未生成可执行步骤");
        }
        return plan;
    }

    private AgentExecutionPlan parseExecutionPlan(String raw, String content, AgentPlanningConfig planningConfig) {
        String clean = stripCodeFence(raw);
        try {
            AgentExecutionPlan plan = objectMapper.readValue(clean, AgentExecutionPlan.class);
            if (plan == null) {
                throw new BusinessException("Planner 返回空计划");
            }
            if (!StringUtils.hasText(plan.getGoal())) {
                plan.setGoal(content == null ? "" : content);
            }
            if (!StringUtils.hasText(plan.getSummary())) {
                plan.setSummary("自动规划执行");
            }
            List<PlanStep> source = plan.getSteps();
            List<PlanStep> normalized = new ArrayList<>();
            int max = safeMaxPlanSteps(planningConfig);
            if (source != null) {
                for (int i = 0; i < source.size() && i < max; i++) {
                    PlanStep step = source.get(i);
                    if (step == null) {
                        continue;
                    }
                    if (step.getStepNo() == null || step.getStepNo() <= 0) {
                        step.setStepNo(i + 1);
                    }
                    if (!StringUtils.hasText(step.getStepName())) {
                        step.setStepName("规划步骤-" + (i + 1));
                    }
                    if (!StringUtils.hasText(step.getRiskLevel())) {
                        step.setRiskLevel("LOW");
                    } else {
                        String risk = step.getRiskLevel().trim().toUpperCase();
                        if (!"LOW".equals(risk) && !"MEDIUM".equals(risk) && !"HIGH".equals(risk)) {
                            risk = "LOW";
                        }
                        step.setRiskLevel(risk);
                    }
                    if (step.getEnableTools() == null) {
                        step.setEnableTools(false);
                    }
                    if (step.getAllowedToolKeys() == null) {
                        step.setAllowedToolKeys(List.of());
                    }
                    normalized.add(step);
                }
            }
            if (normalized.isEmpty()) {
                throw new BusinessException("Planner 未生成有效步骤");
            }
            plan.setSteps(normalized);
            return plan;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Planner 输出解析失败，请检查规划模型与提示词");
        }
    }

    /**
     * 创建计划审批单。
     * 
     * @param run 运行记录。
     * @param plan 执行计划。
     * @param planningConfig 规划配置。
     * @return 审批申请记录。
     */
    private ApprovalRequest createPlanApproval(AgentRun run, AgentExecutionPlan plan, AgentPlanningConfig planningConfig) {
        if (approvalRequestRepository == null || run == null || plan == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        int expireMinutes = planningConfig == null || planningConfig.getApprovalExpireMinutes() == null
                ? 120 : planningConfig.getApprovalExpireMinutes();
        String riskLevel = resolvePlanRiskLevel(plan);
        String summary = StringUtils.hasText(plan.getSummary()) ? plan.getSummary() : "自动规划待确认";
        String snapshotJson;
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("plan", plan);
            snapshot.put("summary", summary);
            snapshot.put("stepCount", plan.getSteps() == null ? 0 : plan.getSteps().size());
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            snapshotJson = "{}";
        }
        ApprovalRequest request = ApprovalRequest.builder()
                .approvalType(PLAN_APPROVAL_TYPE)
                .status("PENDING")
                .runId(run.getRunId())
                .agentId(run.getAgentId())
                .agentVersionId(run.getAgentVersionId())
                .requesterId(run.getOperatorId())
                .requesterType(StringUtils.hasText(run.getOperatorType()) ? run.getOperatorType() : "system")
                .requestReason("Planning 计划待确认: " + summary)
                .toolKey(PLAN_APPROVAL_TOOL_KEY)
                .riskLevel(riskLevel)
                .argumentsSnapshotJson(snapshotJson)
                .argumentsDigest("steps=" + (plan.getSteps() == null ? 0 : plan.getSteps().size()))
                .expireAt(now.plusMinutes(Math.max(expireMinutes, 5)))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return approvalRequestRepository.insert(request);
    }

    /**
     * 解析计划整体风险等级。
     * 
     * @param plan 执行计划。
     * @return 计划风险等级（LOW/MEDIUM/HIGH）。
     */
    private String resolvePlanRiskLevel(AgentExecutionPlan plan) {
        if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
            return "HIGH";
        }
        int score = 1;
        for (PlanStep step : plan.getSteps()) {
            if (step == null || !StringUtils.hasText(step.getRiskLevel())) {
                continue;
            }
            String risk = step.getRiskLevel().trim().toUpperCase();
            if ("HIGH".equals(risk)) {
                score = Math.max(score, 3);
            } else if ("MEDIUM".equals(risk)) {
                score = Math.max(score, 2);
            } else {
                score = Math.max(score, 1);
            }
        }
        return score >= 3 ? "HIGH" : (score == 2 ? "MEDIUM" : "LOW");
    }

    /**
     * 构建待审批阶段的步骤轨迹。
     * 
     * @param plan 执行计划。
     * @param approvalRequestId 审批申请ID。
     * @return 步骤集合。
     */
    private List<PlatformContractV1.StepTrace> buildPlannedStepTraces(AgentExecutionPlan plan, Long approvalRequestId) {
        if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
            return List.of();
        }
        List<PlatformContractV1.StepTrace> traces = new ArrayList<>();
        for (int i = 0; i < plan.getSteps().size(); i++) {
            PlanStep step = plan.getSteps().get(i);
            traces.add(PlatformContractV1.StepTrace.builder()
                    .nodeKey("agent.plan." + (i + 1))
                    .nodeType("PLAN_STEP")
                    .nodeName(normalizePlanningStepName(step, i + 1))
                    .status("PENDING_APPROVAL")
                    .approvalRequestId(approvalRequestId)
                    .outputText(step == null ? "" : truncate(step.getExpectedOutput(), 500))
                    .outputTruncated(step != null && step.getExpectedOutput() != null && step.getExpectedOutput().length() > 500)
                    .build());
        }
        return traces;
    }

    /**
     * 构建规划执行步骤轨迹。
     * 
     * @param step 当前执行步骤。
     * @param index 步骤序号。
     * @param status 状态值。
     * @param costMs 耗时（毫秒）。
     * @param contract 协议结果。
     * @param approvalRequestId 审批申请ID。
     * @param errorMessage 错误信息。
     * @return 步骤轨迹。
     */
    private PlatformContractV1.StepTrace buildPlanningExecutionStepTrace(PlanStep step,
                                                                         int index,
                                                                         String status,
                                                                         long costMs,
                                                                         PlatformContractV1 contract,
                                                                         Long approvalRequestId,
                                                                         String errorMessage) {
        String output = contract == null ? "" : (contract.getAnswer() == null ? "" : contract.getAnswer());
        boolean truncated = output != null && output.length() > 16000;
        if (truncated) {
            output = output.substring(0, 16000);
        }
        return PlatformContractV1.StepTrace.builder()
                .nodeKey("agent.plan." + index)
                .nodeType("PLAN_STEP")
                .nodeName(normalizePlanningStepName(step, index))
                .status(status)
                .costMs(costMs)
                .approvalRequestId(approvalRequestId)
                .errorMessage(errorMessage == null ? null : truncate(errorMessage, 1000))
                .outputText(output)
                .outputTruncated(truncated)
                .build();
    }

    /**
     * 归一化规划步骤名称。
     * 
     * @param step 当前执行步骤。
     * @param fallbackIndex 回退序号。
     * @return 名称文本。
     */
    private String normalizePlanningStepName(PlanStep step, int fallbackIndex) {
        if (step == null || !StringUtils.hasText(step.getStepName())) {
            return "规划步骤-" + fallbackIndex;
        }
        return step.getStepName().trim();
    }

    private String buildPlanningStepInput(AgentExecutionPlan plan, String previousOutput, PlanStep step, int index) {
        StringBuilder sb = new StringBuilder();
        if (plan != null && StringUtils.hasText(plan.getGoal())) {
            sb.append("任务目标").append(plan.getGoal()).append('\n');
        }
        if (step != null && StringUtils.hasText(step.getExpectedOutput())) {
            sb.append("本步期望").append(step.getExpectedOutput()).append('\n');
        }
        sb.append("步骤序号").append(index).append('\n');
        sb.append("当前可用输入").append(previousOutput == null ? "" : previousOutput);
        return sb.toString();
    }

    /**
     * 解析规划阶段使用的模型。
     * 
     * @param planningConfig 规划配置。
     * @return 解析后的模型配置。
     */
    private ModelConfig resolvePlannerModel(AgentPlanningConfig planningConfig) {
        if (planningConfig != null && planningConfig.getPlannerModelId() != null) {
            ModelConfig model = modelConfigService.queryModelConfigById(new IdQuery(planningConfig.getPlannerModelId()));
            if (model == null) {
                throw new BusinessException("planning.plannerModelId 不存在，id=" + planningConfig.getPlannerModelId());
            }
            if (!Boolean.TRUE.equals(model.getEnabled())) {
                throw new BusinessException("planning.plannerModelId 未启用，id=" + planningConfig.getPlannerModelId());
            }
            return model;
        }
        List<ModelConfig> enabled = modelConfigService.queryEnabledModels(new EnabledQuery(true));
        if (enabled == null || enabled.isEmpty()) {
            throw new BusinessException("未配置可用模型");
        }
        return enabled.stream()
                .filter(item -> item != null && item.getId() != null)
                .sorted(Comparator.comparingLong(ModelConfig::getId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未配置可用模型"));
    }

    /**
     * 解析规划步骤使用的模型。
     * 
     * @param step 当前执行步骤。
     * @return 解析后的模型配置。
     */
    private ModelConfig resolvePlanningStepModel(PlanStep step) {
        if (step != null && step.getModelId() != null) {
            ModelConfig model = modelConfigService.queryModelConfigById(new IdQuery(step.getModelId()));
            if (model == null) {
                throw new BusinessException("Planning 步骤模型不存在，modelId=" + step.getModelId());
            }
            if (!Boolean.TRUE.equals(model.getEnabled())) {
                throw new BusinessException("Planning 步骤模型未启用，modelId=" + step.getModelId());
            }
            return model;
        }
        List<ModelConfig> enabled = modelConfigService.queryEnabledModels(new EnabledQuery(true));
        if (enabled == null || enabled.isEmpty()) {
            throw new BusinessException("未配置可用模型");
        }
        return enabled.stream()
                .filter(item -> item != null && item.getId() != null)
                .sorted(Comparator.comparingLong(ModelConfig::getId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未配置可用模型"));
    }

    /**
     * 解析规划配置。
     * 
     * @param version 工作流版本。
     * @return 规划配置。
     */
    private AgentPlanningConfig parsePlanningConfig(AgentVersion version) {
        if (version == null || !StringUtils.hasText(version.getPlanningConfigJson())) {
            return AgentPlanningConfig.builder().enabled(false).build();
        }
        try {
            AgentPlanningConfig config = objectMapper.readValue(version.getPlanningConfigJson(), AgentPlanningConfig.class);
            if (config == null) {
                return AgentPlanningConfig.builder().enabled(false).build();
            }
            if (config.getEnabled() == null) {
                config.setEnabled(false);
            }
            if (config.getRequireHumanConfirm() == null) {
                config.setRequireHumanConfirm(true);
            }
            if (config.getMaxPlanSteps() == null) {
                config.setMaxPlanSteps(6);
            }
            if (config.getReplanMaxTimes() == null) {
                config.setReplanMaxTimes(1);
            }
            if (config.getStepTimeoutMs() == null) {
                config.setStepTimeoutMs(60000);
            }
            if (config.getApprovalExpireMinutes() == null) {
                config.setApprovalExpireMinutes(120);
            }
            return config;
        } catch (Exception e) {
            throw new BusinessException("planningConfigJson 解析失败，请检查配置");
        }
    }

    /**
     * 解析安全的最大规划步数。
     * 
     * @param planningConfig 规划配置。
     * @return 限制后的最大规划步数。
     */
    private int safeMaxPlanSteps(AgentPlanningConfig planningConfig) {
        if (planningConfig == null || planningConfig.getMaxPlanSteps() == null) {
            return 6;
        }
        return Math.max(1, Math.min(planningConfig.getMaxPlanSteps(), 20));
    }

    /**
     * 从运行快照读取执行计划。
     * 
     * @param snapshot 运行快照。
     * @return 执行计划。
     */
    private AgentExecutionPlan readPlanFromSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            throw new BusinessException("planning 快照为空");
        }
        Object planValue = snapshot.get("plan");
        if (planValue == null) {
            throw new BusinessException("planning 快照缺少 plan");
        }
        AgentExecutionPlan plan = objectMapper.convertValue(planValue, AgentExecutionPlan.class);
        if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
            throw new BusinessException("planning 快照中的 plan 无效");
        }
        return plan;
    }

    /**
     * 读取JSON映射数据。
     * 
     * @param json JSON 字符串。
     */
    private Map<String, Object> readJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 移除代码块围栏标记。
     * 
     * @param text 原始文本。
     * @return 去除围栏后的文本。
     */
    private String stripCodeFence(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstBreak = trimmed.indexOf('\n');
            if (firstBreak > -1) {
                trimmed = trimmed.substring(firstBreak + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 将对象安全转换为Long。
     * 
     * @param value 值。
     * @return 转换后的 Long 值，无法转换时返回 `null`。
     */
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 加载智能体信息。
     * 
     * @param agentCode 智能体编码。
     * @return 智能体信息。
     */
    private Agent loadAgent(String agentCode) {
        if (!StringUtils.hasText(agentCode)) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        Agent agent = agentRepository
                .findByCode(new AgentCodeQuery(agentCode))
                .orElseThrow(() -> new NotFoundException("Agent 不存在，agentCode: " + agentCode));
        if (!"ENABLED".equalsIgnoreCase(agent.getStatus())) {
            throw new BusinessException("Agent 已禁用，不可调用，agentCode: " + agentCode);
        }
        if (agent.getCurrentPublishedVersionId() == null) {
            throw new BusinessException("Agent 尚未发布版本，不可调用，agentCode: " + agentCode);
        }
        return agent;
    }

    private AgentVersion loadPublishedVersion(Agent agent) {
        AgentVersion version = agentVersionRepository
                .findById(new AgentVersionIdQuery(agent.getCurrentPublishedVersionId()))
                .orElseThrow(() -> new NotFoundException("发布版本不存在，id: " + agent.getCurrentPublishedVersionId()));
        if (!"PUBLISHED".equalsIgnoreCase(version.getState())) {
            throw new BusinessException("当前版本非 PUBLISHED，不可调用");
        }
        return version;
    }

    /**
     * 解析模型版本。
     * 
     * @param version 工作流版本。
     * @return 解析后的模型配置。
     */
    private ModelConfig resolveModelForVersion(AgentVersion version) {
        if (version == null) {
            return null;
        }
        // 单组织简化从启用模型中选第一个（按 id 升序）
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

    private ParsedOutput callOnce(String runId, ModelConfig model, AgentVersion version, Long sessionId, String userContent) {
        return callOnce(runId, model, version, sessionId, userContent, null);
    }

    private ParsedOutput callOnce(String runId, ModelConfig model, AgentVersion version, Long sessionId, String userContent, ResolvedRag rag) {
        Set<String> allowedToolKeys = parseAllowedToolKeys(version == null ? null : version.getAllowedToolKeysJson());
        return callOnce(runId, model, version, sessionId, userContent, rag, null, allowedToolKeys, true);
    }

    /**
     * 执行单次模型调用。
     * 
     * @param runId 运行ID。
     * @param model 当前模型配置。
     * @param version 工作流版本。
     * @param sessionId 会话 ID。
     * @param userContent 用户输入文本。
     * @param rag RAG配置。
     * @param systemPromptOverride 系统提示词覆盖文本。
     * @param allowedToolKeys 允许调用的工具Key列表。
     * @param enableTools 是否启用工具调用。
     * @return 结构化输出结果。
     */
    private ParsedOutput callOnce(String runId,
                                  ModelConfig model,
                                  AgentVersion version,
                                  Long sessionId,
                                  String userContent,
                                  ResolvedRag rag,
                                  String systemPromptOverride,
                                  Set<String> allowedToolKeys,
                                  boolean enableTools) {
        Set<String> normalizedAllowedToolKeys = allowedToolKeys == null ? Set.of() : allowedToolKeys;
        ChatClient chatClient = buildChatClient(runId, model, version, sessionId, enableTools);
        Prompt prompt = buildPromptWithContract(version, systemPromptOverride, userContent, rag);
        Long modelId = model == null ? null : model.getId();
        Long agentVersionId = version == null ? null : version.getId();
        try {
            if (modelId != null) {
                GatewayToolBindingContextHolder.set(modelId, sessionId, agentVersionId, runId, normalizedAllowedToolKeys);
            }
            ChatResponse resp = chatClient.prompt(prompt).call().chatResponse();
            String raw = extractText(resp);
            ParsedOutput parsed = parseOrRepair(model, version, sessionId, raw);
            applyRagOverrides(parsed, rag);
            return parsed;
        } finally {
            GatewayToolBindingContextHolder.clear();
        }
    }

    private ChatClient buildChatClient(String runId, ModelConfig modelConfig, AgentVersion version, Long sessionId) {
        return buildChatClient(runId, modelConfig, version, sessionId, true);
    }

    /**
     * 构建对话客户端。
     * 
     * @param runId 运行ID。
     * @param modelConfig 模型配置。
     * @param version 工作流版本。
     * @param sessionId 会话 ID。
     * @param enableTools 是否启用工具调用。
     * @return 对话客户端。
     */
    private ChatClient buildChatClient(String runId,
                                       ModelConfig modelConfig,
                                       AgentVersion version,
                                       Long sessionId,
                                       boolean enableTools) {
        if (modelConfig == null) {
            throw new BusinessException("未找到可用模型");
        }
        boolean modelToolEnabled = modelConfig.getToolEnabled() == null || Boolean.TRUE.equals(modelConfig.getToolEnabled());
        boolean resolvedEnableTools = enableTools && modelToolEnabled;
        Long agentVersionId = version == null ? null : version.getId();
        CallAdvisor[] extra = agentVersionId == null
                ? new CallAdvisor[0]
                : agentEnhancerRuntimeService.resolveForAgentVersion(agentVersionId, runId, sessionId);
        return chatClientAssemblyService.buildChatClient(modelConfig, resolvedEnableTools, extra);
    }

    /**
     * 构建不带工具能力的对话客户端。
     * 
     * @param modelConfig 模型配置。
     * @return 对话客户端。
     */
    private ChatClient buildChatClientNoTools(ModelConfig modelConfig) {
        if (modelConfig == null) {
            throw new BusinessException("未找到可用模型");
        }
        return chatClientAssemblyService.buildChatClientNoTools(modelConfig);
    }

    private Prompt buildPromptWithContract(AgentVersion version, String userContent) {
        return buildPromptWithContract(version, null, userContent, null);
    }

    private Prompt buildPromptWithContract(AgentVersion version, String userContent, ResolvedRag rag) {
        return buildPromptWithContract(version, null, userContent, rag);
    }

    private Prompt buildPromptWithContract(AgentVersion version, String systemPromptOverride, String userContent, ResolvedRag rag) {
        String system = StringUtils.hasText(systemPromptOverride) ? systemPromptOverride : version.getSystemPromptSnapshot();
        if (!StringUtils.hasText(system)) {
            system = "";
        }
        String user = userContent == null ? "" : userContent;
        if (rag == null || rag.documents() == null || rag.documents().isEmpty()) {
            return new Prompt(
                    new SystemMessage(system),
                    new SystemMessage(outputSupport.contractInstruction()),
                    new UserMessage(user)
            );
        }
        String ragText = formatRagDocuments(rag.documents());
        String ragSystem = """
                你可以参考以下【参考文档】回答用户问题。
                如果【参考文档】中找不到答案，请在 uncertainty 字段明确说明不确定，不要编造。

                【参考文档】
                """ + ragText;
        return new Prompt(
                new SystemMessage(system),
                new SystemMessage(ragSystem),
                new SystemMessage(outputSupport.contractInstruction()),
                new UserMessage(user)
        );
    }

    /**
     * 将 RAG 文档拼接为模型可读上下文。
     * 
     * @param docs 文档列表。
     * @return 拼接后的文档上下文文本。
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
     * @param parsed 解析后的结构化结果。
     * @param rag RAG配置。
     */
    private void applyRagOverrides(ParsedOutput parsed, ResolvedRag rag) {
        if (parsed == null || parsed.contract == null || rag == null) {
            return;
        }
        if (rag.citations() != null && !rag.citations().isEmpty()) {
            parsed.contract.setCitations(rag.citations());
        }
        if (rag.droppedTags() != null && !rag.droppedTags().isEmpty()) {
            List<String> actions = parsed.contract.getActionsNext();
            if (actions == null) {
                actions = new ArrayList<>();
                parsed.contract.setActionsNext(actions);
            }
            actions.add("RAG 标签未在白名单内，已忽略: " + String.join(",", rag.droppedTags()));
        }
    }

    /**
     * 构建 RAG 必需且未命中的协议结果。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param version 工作流版本。
     * @param costMs 耗时（毫秒）。
     * @param rag RAG配置。
     * @return 构建后的平台协议结果。
     */
    private PlatformContractV1 buildRagRequiredNoHitContract(String runId, 
                                                             String agentCode,
                                                             AgentVersion version,
                                                             long costMs,
                                                             ResolvedRag rag) {
        String reason = rag == null ? null : rag.missReason();
        String uncertainty = StringUtils.hasText(reason)
                ? ("RAG(REQUIRED) 未命中，无法确定答案" + reason)
                : "RAG(REQUIRED) 未命中，无法确定答案。";
        List<String> actions = new ArrayList<>();
        if (rag != null && rag.effectiveTags() != null && !rag.effectiveTags().isEmpty()) {
            actions.add("请确认知识库标签是否正确effectiveTags=" + String.join(",", rag.effectiveTags()));
        } else {
            actions.add("请在 AgentVersion 配置 defaultRagTagsJson/allowedRagTagsJson，或在请求中携带 ragTagsJson。");
        }
        return PlatformContractV1.builder()
                .meta(PlatformContractV1.Meta.builder()
                        .runId(runId)
                        .agentCode(agentCode)
                        .agentVersionId(version == null ? null : version.getId())
                        .agentVersionNo(version == null ? null : version.getVersionNo())
                        .modelUsed(null)
                        .costMs(costMs)
                        .repairAttempts(0)
                        .build())
                .status("SUCCESS")
                .answer("我不太清楚这个问题。")
                .uncertainty(uncertainty)
                .citations(List.of())
                .toolCalls(List.of())
                .actionsNext(actions)
                .build();
    }

    /**
     * 提取文本。
     * 
     * @param response 模型响应内容。
     * @return 处理后的文本内容。
     */
    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private String extractDelta(ChatResponse response) {
        return extractText(response);
    }

    /**
     * 构建成功协议结果。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param version 工作流版本。
     * @param model 当前模型配置。
     * @param parsed 解析后的结构化结果。
     * @param costMs 耗时（毫秒）。
     * @return 成功执行后的平台协议结果。
     */
    private PlatformContractV1 buildSuccessContract(String runId, 
                                                    String agentCode,
                                                    AgentVersion version,
                                                    ModelConfig model,
                                                    ParsedOutput parsed,
                                                    long costMs) {
        return PlatformContractV1.builder()
                .meta(PlatformContractV1.Meta.builder()
                        .runId(runId)
                        .agentCode(agentCode)
                        .agentVersionId(version.getId())
                        .agentVersionNo(version.getVersionNo())
                        .modelUsed(model == null ? null : model.getModelName())
                        .costMs(costMs)
                        .repairAttempts(parsed == null ? 0 : parsed.repairAttempts)
                        .build())
                .status("SUCCESS")
                .answer(parsed == null ? "" : parsed.contract.getAnswer())
                .uncertainty(parsed == null ? "" : parsed.contract.getUncertainty())
                .citations(parsed == null ? List.of() : parsed.contract.getCitations())
                .toolCalls(parsed == null ? List.of() : parsed.contract.getToolCalls())
                .actionsNext(parsed == null ? List.of() : parsed.contract.getActionsNext())
                // Agent 普通调用也返回 stepsP0 先给最小可用的“单步 LLM”明细
                .steps(List.of(buildAgentSingleStep("SUCCESS", costMs, parsed == null ? null : parsed.contract)))
                .build();
    }

    /**
     * 构建Success协议结果Steps。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param version 工作流版本。
     * @param model 当前模型配置。
     * @param parsed 解析后的结构化结果。
     * @param costMs 耗时（毫秒）。
     * @param repairAttempts 重试次数。
     * @param stepTraces 步骤轨迹列表。
     * @return 成功执行后的平台协议结果。
     */
    private PlatformContractV1 buildSuccessContractWithSteps(String runId,
                                                             String agentCode,
                                                             AgentVersion version,
                                                             ModelConfig model,
                                                             ParsedOutput parsed,
                                                             long costMs,
                                                             int repairAttempts,
                                                             List<PlatformContractV1.StepTrace> stepTraces) {
        List<PlatformContractV1.StepTrace> traces = stepTraces == null || stepTraces.isEmpty()
                ? List.of(buildAgentSingleStep("SUCCESS", costMs, parsed == null ? null : parsed.contract))
                : stepTraces;
        return PlatformContractV1.builder()
                .meta(PlatformContractV1.Meta.builder()
                        .runId(runId)
                        .agentCode(agentCode)
                        .agentVersionId(version == null ? null : version.getId())
                        .agentVersionNo(version == null ? null : version.getVersionNo())
                        .modelUsed(model == null ? null : model.getModelName())
                        .costMs(costMs)
                        .repairAttempts(repairAttempts)
                        .build())
                .status("SUCCESS")
                .answer(parsed == null || parsed.contract == null ? "" : parsed.contract.getAnswer())
                .uncertainty(parsed == null || parsed.contract == null ? "" : parsed.contract.getUncertainty())
                .citations(parsed == null || parsed.contract == null ? List.of() : parsed.contract.getCitations())
                .toolCalls(parsed == null || parsed.contract == null ? List.of() : parsed.contract.getToolCalls())
                .actionsNext(parsed == null || parsed.contract == null ? List.of() : parsed.contract.getActionsNext())
                .steps(traces)
                .build();
    }

    /**
     * 构建失败协议结果。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param version 工作流版本。
     * @param model 当前模型配置。
     * @param costMs 耗时（毫秒）。
     * @param message 提示信息。
     * @param repairAttempts 重试次数。
     * @return 构建后的平台协议结果。
     */
    private PlatformContractV1 buildFailedContract(String runId, 
                                                   String agentCode,
                                                   AgentVersion version,
                                                   ModelConfig model,
                                                   long costMs,
                                                   String message,
                                                   int repairAttempts) {
        return PlatformContractV1.builder()
                .meta(PlatformContractV1.Meta.builder()
                        .runId(runId)
                        .agentCode(agentCode)
                        .agentVersionId(version == null ? null : version.getId())
                        .agentVersionNo(version == null ? null : version.getVersionNo())
                        .modelUsed(model == null ? null : model.getModelName())
                        .costMs(costMs)
                        .repairAttempts(repairAttempts)
                        .build())
                .status("FAILED")
                .answer("")
                .uncertainty("")
                .error(PlatformContractV1.Error.builder()
                        .code("AGENT_RUNTIME_FAILED")
                        .message("Agent 运行失败")
                        .detail(message)
                        .build())
                .steps(List.of(PlatformContractV1.StepTrace.builder()
                        .nodeKey("agent.llm")
                        .nodeType("LLM")
                        .nodeName("Agent LLM")
                        .status("FAILED")
                        .costMs(costMs)
                        .errorMessage(truncate(message, 1000))
                        .build()))
                .build();
    }

    /**
     * 构建Failed协议结果Steps。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param version 工作流版本。
     * @param model 当前模型配置。
     * @param costMs 耗时（毫秒）。
     * @param message 提示信息。
     * @param repairAttempts 重试次数。
     * @param stepTraces 步骤轨迹列表。
     * @return 构建后的平台协议结果。
     */
    private PlatformContractV1 buildFailedContractWithSteps(String runId,
                                                            String agentCode,
                                                            AgentVersion version,
                                                            ModelConfig model,
                                                            long costMs,
                                                            String message,
                                                            int repairAttempts,
                                                            List<PlatformContractV1.StepTrace> stepTraces) {
        List<PlatformContractV1.StepTrace> traces = stepTraces == null || stepTraces.isEmpty()
                ? List.of(PlatformContractV1.StepTrace.builder()
                .nodeKey("agent.llm")
                .nodeType("LLM")
                .nodeName("Agent LLM")
                .status("FAILED")
                .costMs(costMs)
                .errorMessage(truncate(message, 1000))
                .build())
                : stepTraces;
        return PlatformContractV1.builder()
                .meta(PlatformContractV1.Meta.builder()
                        .runId(runId)
                        .agentCode(agentCode)
                        .agentVersionId(version == null ? null : version.getId())
                        .agentVersionNo(version == null ? null : version.getVersionNo())
                        .modelUsed(model == null ? null : model.getModelName())
                        .costMs(costMs)
                        .repairAttempts(repairAttempts)
                        .build())
                .status("FAILED")
                .answer("")
                .uncertainty("")
                .error(PlatformContractV1.Error.builder()
                        .code("AGENT_RUNTIME_FAILED")
                        .message("Agent 运行失败")
                        .detail(message)
                        .build())
                .steps(traces)
                .build();
    }

    /**
     * 构建待审批协议结果。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param version 工作流版本。
     * @param model 当前模型配置。
     * @param costMs 耗时（毫秒）。
     * @param approval 审批记录。
     * @return 构建后的平台协议结果。
     */
    private PlatformContractV1 buildPendingApprovalContract(String runId, 
                                                            String agentCode,
                                                            AgentVersion version,
                                                            ModelConfig model,
                                                            long costMs,
                                                            ApprovalRequiredException approval) {
        return PlatformContractV1.builder()
                .meta(PlatformContractV1.Meta.builder()
                        .runId(runId)
                        .agentCode(agentCode)
                        .agentVersionId(version == null ? null : version.getId())
                        .agentVersionNo(version == null ? null : version.getVersionNo())
                        .modelUsed(model == null ? null : model.getModelName())
                        .costMs(costMs)
                        .repairAttempts(0)
                        .approvalRequestId(approval == null ? null : approval.getApprovalRequestId())
                        .pendingToolKey(approval == null ? null : approval.getToolKey())
                        .riskLevel(approval == null ? null : approval.getRiskLevel())
                        .build())
                .status("PENDING_APPROVAL")
                .answer("")
                .uncertainty("")
                .steps(List.of(PlatformContractV1.StepTrace.builder()
                        .nodeKey("agent.llm")
                        .nodeType("LLM")
                        .nodeName("Agent LLM")
                        .status("PENDING_APPROVAL")
                        .costMs(costMs)
                        .approvalRequestId(approval == null ? null : approval.getApprovalRequestId())
                        .errorMessage(approval == null ? null : truncate(approval.getMessage(), 1000))
                        .build()))
                .build();
    }

    /**
     * 构建Pending审批协议结果Steps。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param version 工作流版本。
     * @param model 当前模型配置。
     * @param costMs 耗时（毫秒）。
     * @param approval 审批记录。
     * @param stepTraces 步骤轨迹列表。
     * @return 构建后的平台协议结果。
     */
    private PlatformContractV1 buildPendingApprovalContractWithSteps(String runId,
                                                                     String agentCode,
                                                                     AgentVersion version,
                                                                     ModelConfig model,
                                                                     long costMs,
                                                                     ApprovalRequiredException approval,
                                                                     List<PlatformContractV1.StepTrace> stepTraces) {
        List<PlatformContractV1.StepTrace> traces = stepTraces == null || stepTraces.isEmpty()
                ? List.of(PlatformContractV1.StepTrace.builder()
                .nodeKey("agent.llm")
                .nodeType("LLM")
                .nodeName("Agent LLM")
                .status("PENDING_APPROVAL")
                .costMs(costMs)
                .approvalRequestId(approval == null ? null : approval.getApprovalRequestId())
                .errorMessage(approval == null ? null : truncate(approval.getMessage(), 1000))
                .build())
                : stepTraces;
        return PlatformContractV1.builder()
                .meta(PlatformContractV1.Meta.builder()
                        .runId(runId)
                        .agentCode(agentCode)
                        .agentVersionId(version == null ? null : version.getId())
                        .agentVersionNo(version == null ? null : version.getVersionNo())
                        .modelUsed(model == null ? null : model.getModelName())
                        .costMs(costMs)
                        .repairAttempts(0)
                        .approvalRequestId(approval == null ? null : approval.getApprovalRequestId())
                        .pendingToolKey(approval == null ? null : approval.getToolKey())
                        .riskLevel(approval == null ? null : approval.getRiskLevel())
                        .build())
                .status("PENDING_APPROVAL")
                .answer("")
                .uncertainty("")
                .steps(traces)
                .build();
    }

    private PlatformContractV1.StepTrace buildAgentSingleStep(String status, long costMs, PlatformContractV1 contract) {
        String out = contract == null ? "" : (contract.getAnswer() == null ? "" : contract.getAnswer());
        boolean truncated = out != null && out.length() > 16000;
        if (truncated) {
            out = out.substring(0, 16000);
        }
        return PlatformContractV1.StepTrace.builder()
                .nodeKey("agent.llm")
                .nodeType("LLM")
                .nodeName("Agent LLM")
                .status(status)
                .costMs(costMs)
                .outputText(out)
                .outputTruncated(truncated)
                .build();
    }

    /**
     * 构建链路步骤追踪。
     * 
     * @param step 当前执行步骤。
     * @param index 步骤序号。
     * @param status 状态值。
     * @param costMs 耗时（毫秒）。
     * @param contract 协议结果。
     * @param approvalRequestId 审批申请ID。
     * @param errorMessage 错误信息。
     * @return 步骤轨迹。
     */
    private PlatformContractV1.StepTrace buildChainStepTrace(AgentClientProfileStep step,
                                                             int index,
                                                             String status,
                                                             long costMs,
                                                             PlatformContractV1 contract,
                                                             Long approvalRequestId,
                                                             String errorMessage) {
        String output = contract == null ? "" : (contract.getAnswer() == null ? "" : contract.getAnswer());
        boolean truncated = output != null && output.length() > 16000;
        if (truncated) {
            output = output.substring(0, 16000);
        }
        return PlatformContractV1.StepTrace.builder()
                .nodeKey("agent.chain." + index)
                .nodeType("LLM")
                .nodeName(normalizeChainStepName(step, index))
                .status(status)
                .costMs(costMs)
                .approvalRequestId(approvalRequestId)
                .errorMessage(errorMessage == null ? null : truncate(errorMessage, 1000))
                .outputText(output)
                .outputTruncated(truncated)
                .build();
    }

    /**
     * 归一化链路步骤名称。
     * 
     * @param step 当前执行步骤。
     * @param fallbackIndex 回退序号。
     * @return 名称文本。
     */
    private String normalizeChainStepName(AgentClientProfileStep step, int fallbackIndex) {
        if (step == null || !StringUtils.hasText(step.getStepName())) {
            return "步骤-" + fallbackIndex;
        }
        return step.getStepName().trim();
    }

    /**
     * 解析客户端链路步骤。
     * 
     * @param version 工作流版本。
     * @return 步骤集合。
     */
    private List<AgentClientProfileStep> parseClientChainSteps(AgentVersion version) {
        if (version == null) {
            return List.of();
        }
        if (version.getClientProfileId() != null) {
            List<ClientProfileStep> profileSteps = clientProfileRepository.listSteps(version.getClientProfileId());
            if (profileSteps == null || profileSteps.isEmpty()) {
                throw new BusinessException("ClientProfile 未配置步骤，clientProfileId=" + version.getClientProfileId());
            }
            List<AgentClientProfileStep> normalized = new ArrayList<>();
            for (ClientProfileStep step : profileSteps) {
                if (step == null) {
                    continue;
                }
                AgentClientProfileStep mapped = AgentClientProfileStep.builder()
                        .sequence(step.getSequenceNo())
                        .stepName(step.getStepName())
                        .modelId(step.getModelId())
                        .systemPrompt(step.getSystemPrompt())
                        .enableTools(step.getEnableTools())
                        .allowedToolKeys(parseAllowedToolKeysList(step.getAllowedToolKeysJson()))
                        .build();
                normalized.add(mapped);
            }
            normalized.sort(Comparator.comparingInt(s -> s.getSequence() == null ? Integer.MAX_VALUE : s.getSequence()));
            return normalized;
        }
        return parseClientChainSteps(version.getClientChainJson());
    }

    private boolean hasClientChain(AgentVersion version) {
        return !parseClientChainSteps(version).isEmpty();
    }

    /**
     * 解析客户端链路步骤。
     * 
     * @param clientChainJson 客户端链路JSON。
     * @return 步骤集合。
     */
    private List<AgentClientProfileStep> parseClientChainSteps(String clientChainJson) {
        if (!StringUtils.hasText(clientChainJson)) {
            return List.of();
        }
        try {
            List<AgentClientProfileStep> raw = objectMapper.readValue(clientChainJson, new TypeReference<List<AgentClientProfileStep>>() {});
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            List<AgentClientProfileStep> normalized = new ArrayList<>();
            int sequence = 0;
            for (AgentClientProfileStep step : raw) {
                if (step == null) {
                    continue;
                }
                sequence++;
                if (step.getSequence() == null || step.getSequence() <= 0) {
                    step.setSequence(sequence);
                }
                if (!StringUtils.hasText(step.getStepName())) {
                    step.setStepName("步骤-" + step.getSequence());
                }
                if (step.getModelId() == null) {
                    throw new BusinessException("clientChainJson 存在未配置 modelId 的步骤，step=" + step.getStepName());
                }
                normalized.add(step);
            }
            normalized.sort(Comparator.comparingInt(s -> s.getSequence() == null ? Integer.MAX_VALUE : s.getSequence()));
            return normalized;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("clientChainJson 解析失败，请检查 JSON");
        }
    }

    /**
     * 解析启用模型 ID。
     * 
     * @param modelId 模型ID。
     * @param stepName 步骤名称。
     * @return 解析后的模型配置。
     */
    private ModelConfig resolveEnabledModelById(Long modelId, String stepName) {
        if (modelId == null) {
            throw new BusinessException("步骤模型未配置，step=" + stepName);
        }
        ModelConfig model = modelConfigService.queryModelConfigById(new IdQuery(modelId));
        if (model == null) {
            throw new BusinessException("步骤模型不存在，step=" + stepName + ", modelId=" + modelId);
        }
        if (!Boolean.TRUE.equals(model.getEnabled())) {
            throw new BusinessException("步骤模型未启用，step=" + stepName + ", modelId=" + modelId);
        }
        return model;
    }

    private boolean resolveStepEnableTools(AgentClientProfileStep step, ModelConfig modelConfig) {
        boolean requestedEnableTools = step == null || step.getEnableTools() == null || Boolean.TRUE.equals(step.getEnableTools());
        boolean modelToolEnabled = modelConfig == null || modelConfig.getToolEnabled() == null || Boolean.TRUE.equals(modelConfig.getToolEnabled());
        return requestedEnableTools && modelToolEnabled;
    }

    /**
     * 解析可用工具 Key 列表。
     * 
     * @param step 当前执行步骤。
     * @param version 工作流版本。
     * @return 工具集合。
     */
    private Set<String> resolveAllowedToolKeys(AgentClientProfileStep step, AgentVersion version) {
        if (step != null && step.getAllowedToolKeys() != null && !step.getAllowedToolKeys().isEmpty()) {
            return parseAllowedToolKeys(step.getAllowedToolKeys());
        }
        return parseAllowedToolKeys(version == null ? null : version.getAllowedToolKeysJson());
    }

    /**
     * 保存运行上下文快照。
     * 
     * @param runId 运行ID。
     * @param agentCode 智能体编码。
     * @param agentId 智能体ID。
     * @param version 工作流版本。
     * @param model 当前模型配置。
     * @param sessionId 会话 ID。
     * @param content 用户输入内容。
     * @param ragTagsJson RAG标签JSON。
     */
    private void saveRunContextSnapshot(String runId,
                                       String agentCode,
                                       Long agentId,
                                       AgentVersion version,
                                       ModelConfig model,
                                       Long sessionId,
                                       String content,
                                       String ragTagsJson) {
        saveRunContextSnapshot(runId, agentCode, agentId, version, model, sessionId, content, ragTagsJson, null);
    }

    private void saveRunContextSnapshot(String runId,
                                       String agentCode,
                                       Long agentId,
                                       AgentVersion version,
                                       ModelConfig model,
                                       Long sessionId,
                                       String content,
                                       String ragTagsJson,
                                       Map<String, Object> extras) {
        if (agentRunContextRepository == null || runId == null) {
            return;
        }
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("runId", runId);
            snapshot.put("agentCode", agentCode);
            snapshot.put("agentId", agentId);
            snapshot.put("agentVersionId", version == null ? null : version.getId());
            snapshot.put("agentVersionNo", version == null ? null : version.getVersionNo());
            snapshot.put("modelIdUsed", model == null ? null : model.getId());
            snapshot.put("modelNameUsed", model == null ? null : model.getModelName());
            snapshot.put("sessionId", sessionId);
            snapshot.put("content", content == null ? "" : content);
            snapshot.put("ragTagsJson", ragTagsJson);
            if (extras != null && !extras.isEmpty()) {
                snapshot.putAll(extras);
            }

            String json = objectMapper.writeValueAsString(snapshot);
            AgentRunContext ctx = AgentRunContext.builder()
                    .runId(runId)
                    .status("SAVED")
                    .snapshotJson(json)
                    .build();
            agentRunContextRepository.upsert(ctx);
        } catch (Exception e) {
            log.warn("保存 agent_run_context 快照失败，runId: {}", runId, e);
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private ParsedOutput parseOrRepair(ModelConfig model, AgentVersion version, Long sessionId, String raw) {
        PlatformContractV1 parsed = outputSupport.parseOrNull(raw);
        if (parsed != null) {
            return new ParsedOutput(parsed, 0);
        }

        int maxRetry = version != null && version.getRepairRetryTimes() != null ? version.getRepairRetryTimes() : 2;
        ChatClient repairClient = buildChatClientNoTools(model);
        String current = raw;
        for (int i = 1; i <= maxRetry; i++) {
            String repaired = repairOnce(repairClient, current);
            PlatformContractV1 p = outputSupport.parseOrNull(repaired);
            if (p != null) {
                return new ParsedOutput(p, i);
            }
            current = repaired;
        }
        throw new OutputParseFailedException("模型输出无法解析为 PlatformContractV1 JSON，raw=" + truncate(raw, 600), maxRetry);
    }

    private ParsedOutput parseOrRepairFromStream(ModelConfig model, AgentVersion version, Long sessionId, String raw) {
        return parseOrRepair(model, version, sessionId, raw);
    }

    private String repairOnce(ChatClient repairClient, String invalidOutput) {
        String safe = invalidOutput == null ? "" : invalidOutput;
        Prompt prompt = new Prompt(
                new SystemMessage("你是 JSON 修复器。你必须仅输出合法 JSON，不要输出任何额外文字。"),
                new SystemMessage(outputSupport.contractInstruction()),
                new UserMessage("请将以下内容修复为符合要求的 JSON\n" + safe)
        );
        ChatResponse resp = repairClient.prompt(prompt).call().chatResponse();
        return extractText(resp);
    }

    /**
     * 解析可用工具 Key 列表。
     * 
     * @param json JSON 字符串。
     * @return 工具集合。
     */
    private Set<String> parseAllowedToolKeys(String json) {
        if (!StringUtils.hasText(json)) {
            return Set.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (list == null || list.isEmpty()) {
                return Set.of();
            }
            Set<String> set = new HashSet<>();
            for (String s : list) {
                if (StringUtils.hasText(s)) {
                    set.add(s.trim());
                }
            }
            return set;
        } catch (Exception e) {
            log.warn("解析 allowedToolKeysJson 失败，json: {}", json, e);
            return Set.of();
        }
    }

    /**
     * 解析可用工具 Key 列表。
     * 
     * @param json JSON 字符串。
     * @return 工具集合。
     */
    private List<String> parseAllowedToolKeysList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (String item : list) {
                if (StringUtils.hasText(item)) {
                    result.add(item.trim());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析 step.allowedToolKeysJson 失败，json: {}", json, e);
            return List.of();
        }
    }

    /**
     * 解析可用工具 Key 列表。
     * 
     * @param toolKeys 工具Key列表。
     * @return 工具集合。
     */
    private Set<String> parseAllowedToolKeys(List<String> toolKeys) {
        if (toolKeys == null || toolKeys.isEmpty()) {
            return Set.of();
        }
        Set<String> set = new HashSet<>();
        for (String key : toolKeys) {
            if (StringUtils.hasText(key)) {
                set.add(key.trim());
            }
        }
        return set;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AgentExecutionPlan {

        /**
         * 执行目标。
         */
        private String goal;

        /**
         * 执行摘要。
         */
        private String summary;

        /**
         * 规划步骤列表。
         */
        private List<PlanStep> steps;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class PlanStep {

        /**
         * 步骤序号。
         */
        private Integer stepNo;

        /**
         * 步骤名称。
         */
        private String stepName;

        /**
         * 步骤执行指令。
         */
        private String instruction;

        /**
         * 指定模型 ID。
         */
        private Long modelId;

        /**
         * 是否启用工具。
         */
        private Boolean enableTools;

        /**
         * 允许工具键列表。
         */
        private List<String> allowedToolKeys;

        /**
         * 风险等级。
         */
        private String riskLevel;

        /**
         * 期望输出描述。
         */
        private String expectedOutput;
    }

    private static final class ParsedOutput {

        /**
         * 解析后的平台协议对象。
         */
        private final PlatformContractV1 contract;

        /**
         * 修复尝试次数。
         */
        private final int repairAttempts;

        private ParsedOutput(PlatformContractV1 contract, int repairAttempts) {
            this.contract = contract;
            this.repairAttempts = repairAttempts;
        }
    }

    private static final class OutputParseFailedException extends RuntimeException {

        /**
         * 修复尝试次数。
         */
        private final int repairAttempts;

        private OutputParseFailedException(String message, int repairAttempts) {
            super(message);
            this.repairAttempts = repairAttempts;
        }
    }
}
