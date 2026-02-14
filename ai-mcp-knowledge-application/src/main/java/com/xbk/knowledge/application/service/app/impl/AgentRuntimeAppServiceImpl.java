package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.application.support.contract.PlatformContractV1OutputSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport;
import com.xbk.knowledge.application.support.rag.AgentRagGovernanceSupport.ResolvedRag;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.entity.agent.AgentRun;
import com.xbk.knowledge.domain.model.entity.agent.AgentRunContext;
import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.repository.AgentRepository;
import com.xbk.knowledge.domain.repository.AgentRunRepository;
import com.xbk.knowledge.domain.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.repository.AgentVersionRepository;
import com.xbk.knowledge.domain.service.IModelConfigService;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.contract.PlatformStreamEvent;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.ApprovalRequiredException;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.ai.tool.ToolCallbackProvider;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 运行入口应用服务实现。
 *
 * P0 策略：
 * - 必须按 org+agentCode 找到 Agent
 * - 必须使用当前发布版本（Agent.current_published_version_id）
 * - 输出 Platform Contract v1（先不做结构化解析修复；P1 再加）
 * - 工具调用暂不启用（避免跨 org 工具泄漏；Iteration 3 再做 allowlist + toolKey）
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
    private final ModelProviderFactory modelProviderFactory;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;
    private final PlatformContractV1OutputSupport outputSupport;
    private final AgentRagGovernanceSupport ragGovernanceSupport;

    @Override
    public PlatformContractV1 chat(Long orgId, String agentCode, Long sessionId, String content, String ragTagsJson) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(orgId, agentCode);
        AgentVersion version = loadPublishedVersion(orgId, agent);
        ModelConfig model = resolveModelForVersion(orgId, version);

        OrgContext ctx = OrgContextHolder.get();
        Long operatorId = ctx == null ? null : ctx.operatorUserId();
        String operatorType = operatorId == null ? "system" : "user";

        AgentRun run = AgentRun.builder()
                .runId(runId)
                .orgId(orgId)
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
        saveRunContextSnapshot(orgId, runId, agentCode, agent.getId(), version, model, sessionId, content, ragTagsJson);

        try {
            ResolvedRag rag = ragGovernanceSupport.resolve(version, ragTagsJson, content);
            if (rag != null && rag.requiredMiss()) {
                long costMs = System.currentTimeMillis() - start;
                AgentRun toUpdate = AgentRun.builder()
                        .runId(runId)
                        .orgId(orgId)
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
                return buildRagRequiredNoHitContract(runId, orgId, agentCode, version, costMs, rag);
            }

            ParsedOutput parsed = callOnce(model, version, sessionId, content, rag);
            long costMs = System.currentTimeMillis() - start;

            AgentRun toUpdate = AgentRun.builder()
                    .runId(runId)
                    .orgId(orgId)
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

            return buildSuccessContract(runId, orgId, agentCode, version, model, parsed, costMs);
        } catch (ApprovalRequiredException e) {
            // 高风险工具触发审批：run 进入待审批态，不结束 run（endedAt 置空）
            agentRunRepository.updateStatus(orgId, runId, "PENDING_APPROVAL", null, null);
            long costMs = System.currentTimeMillis() - start;
            return buildPendingApprovalContract(runId, orgId, agentCode, version, model, costMs, e);
        } catch (OutputParseFailedException e) {
            long costMs = System.currentTimeMillis() - start;
            AgentRun toUpdate = AgentRun.builder()
                    .runId(runId)
                    .orgId(orgId)
                    .status("FAILED")
                    .errorMessage(truncate(e.getMessage(), 1000))
                    .repairAttempts(e.repairAttempts)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build();
            agentRunRepository.updateStatusAndMetrics(toUpdate);
            return buildFailedContract(runId, orgId, agentCode, version, model, costMs, e.getMessage(), e.repairAttempts);
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            String msg = e.getMessage();
            AgentRun toUpdate = AgentRun.builder()
                    .runId(runId)
                    .orgId(orgId)
                    .status("FAILED")
                    .errorMessage(truncate(msg, 1000))
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build();
            agentRunRepository.updateStatusAndMetrics(toUpdate);
            return buildFailedContract(runId, orgId, agentCode, version, model, costMs, msg, 0);
        }
    }

    @Override
    public Flux<PlatformStreamEvent> stream(Long orgId, String agentCode, Long sessionId, String content, String ragTagsJson) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(orgId, agentCode);
        AgentVersion version = loadPublishedVersion(orgId, agent);
        ModelConfig model = resolveModelForVersion(orgId, version);

        OrgContext ctx = OrgContextHolder.get();
        Long operatorId = ctx == null ? null : ctx.operatorUserId();
        String operatorType = operatorId == null ? "system" : "user";

        AgentRun run = AgentRun.builder()
                .runId(runId)
                .orgId(orgId)
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
        saveRunContextSnapshot(orgId, runId, agentCode, agent.getId(), version, model, sessionId, content, ragTagsJson);

        AtomicInteger totalTokens = new AtomicInteger(0);

        ResolvedRag rag = ragGovernanceSupport.resolve(version, ragTagsJson, content);
        if (rag != null && rag.requiredMiss()) {
            long costMs = System.currentTimeMillis() - start;
            AgentRun toUpdate = AgentRun.builder()
                    .runId(runId)
                    .orgId(orgId)
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
            PlatformContractV1 finalContract = buildRagRequiredNoHitContract(runId, orgId, agentCode, version, costMs, rag);
            return Flux.just(PlatformStreamEvent.builder().name("final").data(finalContract).build());
        }

        Prompt prompt = buildPromptWithContract(version, content, rag);
        StringBuilder answerBuffer = new StringBuilder();
        Set<String> allowedToolKeys = parseAllowedToolKeys(version == null ? null : version.getAllowedToolKeysJson());

        ChatClient chatClient = buildChatClient(model);
        Long modelId = model == null ? null : model.getId();
        Long agentVersionId = version == null ? null : version.getId();

        return Flux.using(
                        () -> {
                            if (modelId != null) {
                                GatewayToolBindingContextHolder.set(modelId, sessionId, agentVersionId, allowedToolKeys);
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
                        agentRunRepository.updateStatus(orgId, runId, "PENDING_APPROVAL", null, null);
                        return;
                    }
                    long costMs = System.currentTimeMillis() - start;
                    AgentRun toUpdate = AgentRun.builder()
                            .runId(runId)
                            .orgId(orgId)
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
                                .orgId(orgId)
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
                        PlatformContractV1 finalContract = buildSuccessContract(runId, orgId, agentCode, version, model, parsed, costMs);
                        return Flux.just(PlatformStreamEvent.builder().name("final").data(finalContract).build());
                    } catch (OutputParseFailedException e) {
                        AgentRun toUpdate = AgentRun.builder()
                                .runId(runId)
                                .orgId(orgId)
                                .status("FAILED")
                                .errorMessage(truncate(e.getMessage(), 1000))
                                .repairAttempts(e.repairAttempts)
                                .costMs(costMs)
                                .endedAt(LocalDateTime.now())
                                .build();
                        agentRunRepository.updateStatusAndMetrics(toUpdate);
                        PlatformContractV1 failed = buildFailedContract(runId, orgId, agentCode, version, model, costMs, e.getMessage(), e.repairAttempts);
                        return Flux.just(PlatformStreamEvent.builder().name("final").data(failed).build());
                    }
                }))
                .onErrorResume(e -> {
                    long costMs = System.currentTimeMillis() - start;
                    if (e instanceof ApprovalRequiredException approval) {
                        PlatformContractV1 pending = buildPendingApprovalContract(
                                runId,
                                orgId,
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
                            orgId,
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

    @Override
    public PlatformContractV1 invoke(Long orgId, String agentCode, Long sessionId, String content, String ragTagsJson) {
        // 内部触发同样走 current published version
        return chat(orgId, agentCode, sessionId, content, ragTagsJson);
    }

    @Override
    public PlatformContractV1 runJob(Long orgId, String agentCode, String content, String ragTagsJson) {
        // XXL 调度触发：与 chat 的核心逻辑一致，但 runType/triggerSource 固定，并且操作者为 system
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Agent agent = loadAgent(orgId, agentCode);
        AgentVersion version = loadPublishedVersion(orgId, agent);
        ModelConfig model = resolveModelForVersion(orgId, version);

        AgentRun run = AgentRun.builder()
                .runId(runId)
                .orgId(orgId)
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
        saveRunContextSnapshot(orgId, runId, agentCode, agent.getId(), version, model, null, content, ragTagsJson);

        try {
            ResolvedRag rag = ragGovernanceSupport.resolve(version, ragTagsJson, content);
            if (rag != null && rag.requiredMiss()) {
                long costMs = System.currentTimeMillis() - start;
                AgentRun toUpdate = AgentRun.builder()
                        .runId(runId)
                        .orgId(orgId)
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
                return buildRagRequiredNoHitContract(runId, orgId, agentCode, version, costMs, rag);
            }

            ParsedOutput parsed = callOnce(model, version, null, content, rag);
            long costMs = System.currentTimeMillis() - start;
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .orgId(orgId)
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
            return buildSuccessContract(runId, orgId, agentCode, version, model, parsed, costMs);
        } catch (ApprovalRequiredException e) {
            // XXL 模式按约定：Job 不失败；run 进入待审批态
            agentRunRepository.updateStatus(orgId, runId, "PENDING_APPROVAL", null, null);
            long costMs = System.currentTimeMillis() - start;
            return buildPendingApprovalContract(runId, orgId, agentCode, version, model, costMs, e);
        } catch (OutputParseFailedException e) {
            long costMs = System.currentTimeMillis() - start;
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .orgId(orgId)
                    .status("FAILED")
                    .errorMessage(truncate(e.getMessage(), 1000))
                    .repairAttempts(e.repairAttempts)
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build());
            return buildFailedContract(runId, orgId, agentCode, version, model, costMs, e.getMessage(), e.repairAttempts);
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            String msg = e.getMessage();
            agentRunRepository.updateStatusAndMetrics(AgentRun.builder()
                    .runId(runId)
                    .orgId(orgId)
                    .status("FAILED")
                    .errorMessage(truncate(msg, 1000))
                    .costMs(costMs)
                    .endedAt(LocalDateTime.now())
                    .build());
            return buildFailedContract(runId, orgId, agentCode, version, model, costMs, msg, 0);
        }
    }

    private Agent loadAgent(Long orgId, String agentCode) {
        if (orgId == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        if (!StringUtils.hasText(agentCode)) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        Agent agent = agentRepository
                .findByCode(new AgentCodeQuery(orgId, agentCode))
                .orElseThrow(() -> new NotFoundException("Agent 不存在，agentCode: " + agentCode));
        if (!"ENABLED".equalsIgnoreCase(agent.getStatus())) {
            throw new BusinessException("Agent 已禁用，不可调用，agentCode: " + agentCode);
        }
        if (agent.getCurrentPublishedVersionId() == null) {
            throw new BusinessException("Agent 尚未发布版本，不可调用，agentCode: " + agentCode);
        }
        return agent;
    }

    private AgentVersion loadPublishedVersion(Long orgId, Agent agent) {
        AgentVersion version = agentVersionRepository
                .findById(new AgentVersionIdQuery(orgId, agent.getCurrentPublishedVersionId()))
                .orElseThrow(() -> new NotFoundException("发布版本不存在，id: " + agent.getCurrentPublishedVersionId()));
        if (!"PUBLISHED".equalsIgnoreCase(version.getState())) {
            throw new BusinessException("当前版本非 PUBLISHED，不可调用");
        }
        return version;
    }

    private ModelConfig resolveModelForVersion(Long orgId, AgentVersion version) {
        if (version == null) {
            return null;
        }
        // P0：优先 FIXED_MODEL，其次兜底第一个启用模型（后续按任务类型策略扩展）
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
        // 临时兜底：从启用模型中选 priority 最大的（限定 org）
        List<ModelConfig> enabled = modelConfigService.queryEnabledModels(new com.xbk.knowledge.domain.model.vo.common.EnabledQuery(true));
        if (enabled == null || enabled.isEmpty()) {
            throw new BusinessException("未配置可用模型");
        }
        return enabled.stream()
                .filter(m -> m != null && m.getOrgId() != null && orgId != null && orgId.equals(m.getOrgId()))
                .max(Comparator.comparingInt(m -> m.getPriority() == null ? 0 : m.getPriority()))
                .orElseThrow(() -> new BusinessException("当前组织未配置可用模型"));
    }

    private ParsedOutput callOnce(ModelConfig model, AgentVersion version, Long sessionId, String userContent) {
        return callOnce(model, version, sessionId, userContent, null);
    }

    private ParsedOutput callOnce(ModelConfig model, AgentVersion version, Long sessionId, String userContent, ResolvedRag rag) {
        Set<String> allowedToolKeys = parseAllowedToolKeys(version == null ? null : version.getAllowedToolKeysJson());
        ChatClient chatClient = buildChatClient(model);
        Prompt prompt = buildPromptWithContract(version, userContent, rag);
        Long modelId = model == null ? null : model.getId();
        Long agentVersionId = version == null ? null : version.getId();
        try {
            if (modelId != null) {
                GatewayToolBindingContextHolder.set(modelId, sessionId, agentVersionId, allowedToolKeys);
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

    private ChatClient buildChatClient(ModelConfig modelConfig) {
        if (modelConfig == null) {
            throw new BusinessException("未找到可用模型");
        }
        ChatClient base = modelProviderFactory.createChatClient(modelConfig);
        boolean modelToolEnabled = modelConfig.getToolEnabled() == null || Boolean.TRUE.equals(modelConfig.getToolEnabled());
        if (!modelToolEnabled || toolCallbackProvider == null) {
            return base;
        }

        return base.mutate()
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    private ChatClient buildChatClientNoTools(ModelConfig modelConfig) {
        if (modelConfig == null) {
            throw new BusinessException("未找到可用模型");
        }
        return modelProviderFactory.createChatClient(modelConfig);
    }

    private Prompt buildPromptWithContract(AgentVersion version, String userContent) {
        return buildPromptWithContract(version, userContent, null);
    }

    private Prompt buildPromptWithContract(AgentVersion version, String userContent, ResolvedRag rag) {
        String system = version.getSystemPromptSnapshot();
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
                actions = new java.util.ArrayList<>();
                parsed.contract.setActionsNext(actions);
            }
            actions.add("RAG 标签未在白名单内，已忽略: " + String.join(",", rag.droppedTags()));
        }
    }

    private PlatformContractV1 buildRagRequiredNoHitContract(String runId,
                                                             Long orgId,
                                                             String agentCode,
                                                             AgentVersion version,
                                                             long costMs,
                                                             ResolvedRag rag) {
        String reason = rag == null ? null : rag.missReason();
        String uncertainty = StringUtils.hasText(reason)
                ? ("RAG(REQUIRED) 未命中，无法确定答案：" + reason)
                : "RAG(REQUIRED) 未命中，无法确定答案。";
        List<String> actions = new java.util.ArrayList<>();
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
                        .orgId(orgId)
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
                                                    Long orgId,
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
                        .orgId(orgId)
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
                .build();
    }

    private PlatformContractV1 buildFailedContract(String runId,
                                                   Long orgId,
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
                        .orgId(orgId)
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
                .build();
    }

    private PlatformContractV1 buildPendingApprovalContract(String runId,
                                                            Long orgId,
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
                        .orgId(orgId)
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
                .build();
    }

    private void saveRunContextSnapshot(Long orgId,
                                       String runId,
                                       String agentCode,
                                       Long agentId,
                                       AgentVersion version,
                                       ModelConfig model,
                                       Long sessionId,
                                       String content,
                                       String ragTagsJson) {
        if (agentRunContextRepository == null || orgId == null || runId == null) {
            return;
        }
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("orgId", orgId);
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
                    .orgId(orgId)
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
