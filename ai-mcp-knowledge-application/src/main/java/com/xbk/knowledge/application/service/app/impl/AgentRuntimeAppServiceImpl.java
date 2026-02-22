package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.WorkflowRuntimeAppService;
import com.xbk.knowledge.application.service.runtime.AdvisorRuntimeService;
import com.xbk.knowledge.application.support.contract.PlatformContractV1OutputSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport.ResolvedRag;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.entity.AgentRun;
import com.xbk.knowledge.domain.agent.model.entity.AgentRunContext;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
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
 * P0 策略：
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

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentRunContextRepository agentRunContextRepository;
    private final IModelConfigService modelConfigService;
    private final ChatClientAssemblyService chatClientAssemblyService;
    private final AdvisorRuntimeService advisorRuntimeService;
    private final ObjectMapper objectMapper;
    private final PlatformContractV1OutputSupport outputSupport;
    private final AgentRagGovernanceSupport ragGovernanceSupport;
    private final WorkflowRuntimeAppService workflowRuntimeAppService;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowRepository workflowRepository;
    private final ClientProfileRepository clientProfileRepository;

    /**
     * chat。
     *
     * @param agentCode 参数
     * @param sessionId 参数
     * @param content 参数
     * @param ragTagsJson 参数
     * @return 返回结果
     */
    @Override
    public PlatformContractV1 chat(String agentCode, Long sessionId, String content, String ragTagsJson) {
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(agentCode);
        AgentVersion version = loadPublishedVersion(agent);
        List<AgentClientProfileStep> chainSteps = parseClientChainSteps(version);

        // Agent 绑定 Workflow：直接走 WorkflowRuntime（并返回 steps 明细）
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
            // 高风险工具触发审批：run 进入待审批态，不结束 run（endedAt 置空）
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

        // 仍写入 agent_run，便于审计；工具审批会优先按 workflow 归属生成审批单（见 ToolCallbackProvider 的逻辑修正）
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
                throw new BusinessException("Agent clientChain 执行失败：未产出有效结果");
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
     * stream。
     *
     * @param agentCode 参数
     * @param sessionId 参数
     * @param content 参数
     * @param ragTagsJson 参数
     * @return 返回结果
     */
    @Override
    public Flux<PlatformStreamEvent> stream(String agentCode, Long sessionId, String content, String ragTagsJson) {
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(agentCode);
        AgentVersion version = loadPublishedVersion(agent);
        if (version != null && (version.getWorkflowVersionId() != null || hasClientChain(version))) {
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
     * invoke。
     *
     * @param agentCode 参数
     * @param sessionId 参数
     * @param content 参数
     * @param ragTagsJson 参数
     * @return 返回结果
     */
    @Override
    public PlatformContractV1 invoke(String agentCode, Long sessionId, String content, String ragTagsJson) {
        // 内部触发同样走 current published version
        return chat(agentCode, sessionId, content, ragTagsJson);
    }

    /**
     * runJob。
     *
     * @param agentCode 参数
     * @param content 参数
     * @param ragTagsJson 参数
     * @return 返回结果
     */
    @Override
    public PlatformContractV1 runJob(String agentCode, String content, String ragTagsJson) {
        // XXL 调度触发：与 chat 的核心逻辑一致，但 runType/triggerSource 固定，并且操作者为 system
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(agentCode);
        AgentVersion version = loadPublishedVersion(agent);
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
            // XXL 模式按约定：Job 不失败；run 进入待审批态
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

    private ModelConfig resolveModelForVersion(AgentVersion version) {
        if (version == null) {
            return null;
        }
        // 单组织简化：从启用模型中选第一个（按 id 升序）
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
                : advisorRuntimeService.resolveForAgentVersion(agentVersionId, runId, sessionId);
        return chatClientAssemblyService.buildChatClient(modelConfig, resolvedEnableTools, extra);
    }

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

    private PlatformContractV1 buildRagRequiredNoHitContract(String runId, 
                                                             String agentCode,
                                                             AgentVersion version,
                                                             long costMs,
                                                             ResolvedRag rag) {
        String reason = rag == null ? null : rag.missReason();
        String uncertainty = StringUtils.hasText(reason)
                ? ("RAG(REQUIRED) 未命中，无法确定答案：" + reason)
                : "RAG(REQUIRED) 未命中，无法确定答案。";
        List<String> actions = new ArrayList<>();
        if (rag != null && rag.effectiveTags() != null && !rag.effectiveTags().isEmpty()) {
            actions.add("请确认知识库标签是否正确：effectiveTags=" + String.join(",", rag.effectiveTags()));
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
                // Agent 普通调用也返回 steps：P0 先给最小可用的“单步 LLM”明细
                .steps(List.of(buildAgentSingleStep("SUCCESS", costMs, parsed == null ? null : parsed.contract)))
                .build();
    }

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

    private String normalizeChainStepName(AgentClientProfileStep step, int fallbackIndex) {
        if (step == null || !StringUtils.hasText(step.getStepName())) {
            return "步骤-" + fallbackIndex;
        }
        return step.getStepName().trim();
    }

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

    private Set<String> resolveAllowedToolKeys(AgentClientProfileStep step, AgentVersion version) {
        if (step != null && step.getAllowedToolKeys() != null && !step.getAllowedToolKeys().isEmpty()) {
            return parseAllowedToolKeys(step.getAllowedToolKeys());
        }
        return parseAllowedToolKeys(version == null ? null : version.getAllowedToolKeysJson());
    }

    private void saveRunContextSnapshot(String runId,
                                       String agentCode,
                                       Long agentId,
                                       AgentVersion version,
                                       ModelConfig model,
                                       Long sessionId,
                                       String content,
                                       String ragTagsJson) {
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
                new UserMessage("请将以下内容修复为符合要求的 JSON：\n" + safe)
        );
        ChatResponse resp = repairClient.prompt(prompt).call().chatResponse();
        return extractText(resp);
    }

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

    private static final class ParsedOutput {
        private final PlatformContractV1 contract;
        private final int repairAttempts;

        private ParsedOutput(PlatformContractV1 contract, int repairAttempts) {
            this.contract = contract;
            this.repairAttempts = repairAttempts;
        }
    }

    private static final class OutputParseFailedException extends RuntimeException {
        private final int repairAttempts;

        private OutputParseFailedException(String message, int repairAttempts) {
            super(message);
            this.repairAttempts = repairAttempts;
        }
    }
}
