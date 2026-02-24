package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.WorkflowRuntimeAppService;
import com.xbk.knowledge.application.service.runtime.AgentEnhancerRuntimeService;
import com.xbk.knowledge.application.support.contract.PlatformContractV1OutputSupport;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowEdge;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNodeRun;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRun;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRunContext;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowCodeQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowGraphQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowGraphRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowNodeRunRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunContextRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowVersionRepository;
import com.xbk.knowledge.domain.llm.service.IModelConfigService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.exception.ApprovalRequiredException;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.json.JsonMapUtils;
import com.xbk.knowledge.types.tool.ToolKeyAware;
import com.xbk.knowledge.types.tool.ToolInvokeBypassContextHolder;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Workflow 运行面应用服务实现（DAG：当前实现为“可达节点的拓扑就绪队列”，并支持条件边）。
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowRuntimeAppServiceImpl implements WorkflowRuntimeAppService {

    private static final int MAX_OUTPUT_TEXT_CHARS = 16000;
    private static final int MAX_DIGEST_CHARS = 500;

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowGraphRepository workflowGraphRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowRunContextRepository workflowRunContextRepository;
    private final WorkflowNodeRunRepository workflowNodeRunRepository;
    private final ApprovalRequestRepository approvalRequestRepository;

    private final IModelConfigService modelConfigService;
    private final ChatClientAssemblyService chatClientAssemblyService;
    private final AgentEnhancerRuntimeService agentEnhancerRuntimeService;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;
    private final PlatformContractV1OutputSupport outputSupport;
    private final IdentityContextService identityContextService;
    private final RagVectorStoreService ragVectorStoreService;
    // 节点执行器注册表：key=节点类型，value=该类型的执行策略
    // 运行时只做 O(1) 查表，不再走大段 if/else
    private final Map<String, WorkflowNodeExecutor> nodeExecutors = initNodeExecutors();

    /**
     * 初始化节点执行器注册表
     *
     * @return 不可变的节点类型与执行器映射
     */
    private Map<String, WorkflowNodeExecutor> initNodeExecutors() {
        // 使用 LinkedHashMap 保持注册顺序，排查问题时更直观
        Map<String, WorkflowNodeExecutor> executors = new LinkedHashMap<>();

        // 1、流程结构节点：只负责图结构控制，不做业务计算
        registerExecutors(executors, List.of("START", "PARALLEL", "JOIN", "END"), this::executePassThroughNode);

        // 2、业务节点：每个节点类型绑定各自处理函数
        registerExecutor(executors, "RAG_RETRIEVE", this::executeRagRetrieveNode);
        registerExecutor(executors, "IF", this::executeIfNode);
        registerExecutor(executors, "TOOL_CALL", this::executeToolCallNode);

        // 3、模型输出节点：LLM 与 OUTPUT 走同一条执行逻辑
        registerExecutors(executors, List.of("LLM", "OUTPUT"), this::executeLlmOrOutputNode);

        // 返回只读视图，避免运行中被意外改写
        return Collections.unmodifiableMap(executors);
    }

    /**
     * 批量注册节点执行器。
     *
     * @param executors 节点执行器注册表。
     * @param nodeTypes 节点类型列表。
     * @param executor 节点执行器。
     */
    private void registerExecutors(Map<String, WorkflowNodeExecutor> executors,
                                   List<String> nodeTypes,
                                   WorkflowNodeExecutor executor) {
        for (String nodeType : nodeTypes) {
            registerExecutor(executors, nodeType, executor);
        }
    }

    /**
     * 注册单个节点执行器。
     *
     * @param executors 节点执行器注册表。
     * @param nodeType 节点类型。
     * @param executor 节点执行器。
     */
    private void registerExecutor(Map<String, WorkflowNodeExecutor> executors,
                                  String nodeType,
                                  WorkflowNodeExecutor executor) {
        WorkflowNodeExecutor previous = executors.put(nodeType, executor);
        // 同一个节点类型重复注册通常是配置错误，启动时直接失败更安全
        if (previous != null) {
            throw new IllegalStateException("节点类型执行器重复注册: " + nodeType);
        }
    }

    /**
     * 执行主流程并返回协议结果。
     *
     * @param workflowCode 工作流编码。
     * @param sessionId 会话ID。
     * @param content 用户输入内容。
     * @param variablesJson 运行变量JSON。
     * @param workflowVersionId 工作流版本ID。
     * @return 返回平台协议结果对象。
     */
    @Override
    public PlatformContractV1 run(String workflowCode,
                                  Long sessionId,
                                  String content,
                                  String variablesJson,
                                  Long workflowVersionId) {
        if (!StringUtils.hasText(workflowCode)) {
            throw new IllegalArgumentException("workflowCode 不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content 不能为空");
        }

        long start = System.currentTimeMillis();
        String runId = TraceIdUtils.getOrCreateTraceId();

        Workflow wf = workflowRepository.findByCode(WorkflowCodeQuery.builder().workflowCode(workflowCode).build())
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，code=" + workflowCode));
        if (!"ENABLED".equalsIgnoreCase(wf.getStatus())) {
            throw new BusinessException("Workflow 未启用，code=" + workflowCode);
        }

        WorkflowVersion version = resolveVersion(wf, workflowVersionId);
        Graph graph = loadGraph(version.getId());
        validateGraph(graph);

        Long operatorId = null;
        String operatorType = operatorId == null ? "system" : "user";

        WorkflowRun run = WorkflowRun.builder()
                .runId(runId)
                .workflowId(wf.getId())
                .workflowCode(wf.getWorkflowCode())
                .workflowVersionId(version.getId())
                .triggerSource("HTTP")
                .operatorId(operatorId)
                .operatorType(operatorType)
                .sessionId(sessionId)
                .status("RUNNING")
                .currentNodeKey(null)
                .costMs(null)
                .errorMessage(null)
                .startedAt(LocalDateTime.now())
                .endedAt(null)
                .build();
        workflowRunRepository.insert(run);

        Map<String, Object> variables = parseJsonMap(variablesJson);
        Map<String, Object> stepOutputs = new LinkedHashMap<>();
        List<WorkflowNodeRun> nodeRuns = new ArrayList<>();

        try {
            ExecutionResult exec = executeGraph(runId, wf, version, graph, sessionId, content, variables, stepOutputs, nodeRuns);
            long costMs = System.currentTimeMillis() - start;

            if (exec.status.equals("PENDING_APPROVAL")) {
                workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                        .runId(runId)
                        .status("PENDING_APPROVAL")
                        .currentNodeKey(exec.pendingNodeKey)
                        .costMs(costMs)
                        .errorMessage(null)
                        .endedAt(LocalDateTime.now())
                        .build());

                saveRunContextSnapshot(runId, wf, version, sessionId, content, variables, stepOutputs, exec.pendingNodeKey, exec.approvalRequestId, exec.pendingToolKey, exec.pendingRiskLevel);

                return buildContractWithSteps(wf, version, runId, costMs, "PENDING_APPROVAL", exec.contract, nodeRuns, exec.approvalRequestId, exec.pendingToolKey, exec.pendingRiskLevel);
            }

            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(runId)
                    .status(exec.status)
                    .currentNodeKey(null)
                    .costMs(costMs)
                    .errorMessage(exec.status.equals("FAILED") ? exec.errorMessage : null)
                    .endedAt(LocalDateTime.now())
                    .build());

            return buildContractWithSteps(wf, version, runId, costMs, exec.status, exec.contract, nodeRuns, null, null, null);
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(runId)
                    .status("FAILED")
                    .currentNodeKey(null)
                    .costMs(costMs)
                    .errorMessage(truncate(e.getMessage(), 1000))
                    .endedAt(LocalDateTime.now())
                    .build());
            PlatformContractV1 contract = PlatformContractV1.builder()
                    .answer("")
                    .uncertainty("")
                    .citations(List.of())
                    .toolCalls(List.of())
                    .actionsNext(List.of())
                    .build();
            return buildContractWithSteps(wf, version, runId, costMs, "FAILED", contract, nodeRuns, null, null, null);
        }
    }

    /**
     * 在审批通过后恢复工作流运行。
     *
     * @param approvalRequestId 审批单 ID。
     * @return 返回 PlatformContractV1 数据。
     */
    @Override
    public PlatformContractV1 resumeFromApproval(Long approvalRequestId) {
        if (approvalRequestId == null) {
            throw new IllegalArgumentException("approvalRequestId 不能为空");
        }
        ApprovalRequest req = approvalRequestRepository.findById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("审批单不存在，id=" + approvalRequestId));
        if (!"APPROVED".equalsIgnoreCase(req.getStatus())) {
            throw new BusinessException("审批单非已通过状态，不可续跑，status=" + req.getStatus());
        }
        if (!StringUtils.hasText(req.getRunId()) || !StringUtils.hasText(req.getToolKey())) {
            throw new BusinessException("审批单缺少 runId/toolKey");
        }
        if (req.getWorkflowVersionId() == null || req.getWorkflowId() == null || !StringUtils.hasText(req.getNodeKey())) {
            throw new BusinessException("审批单缺少 workflowVersionId/workflowId/nodeKey，无法续跑");
        }

        Workflow wf = workflowRepository.findById(new IdQuery(req.getWorkflowId()))
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，id=" + req.getWorkflowId()));
        WorkflowVersion version = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().id(req.getWorkflowVersionId()).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + req.getWorkflowVersionId()));

        // 读取上下文快照
        WorkflowSnapshot snap = loadSnapshot(req.getRunId());
        if (snap == null || snap.sessionId == null || !StringUtils.hasText(snap.content)) {
            // 快照缺失不阻断：用空输入兜底
            snap = snap == null ? new WorkflowSnapshot(null, "", new LinkedHashMap<>(), new LinkedHashMap<>(), req.getNodeKey()) : snap;
        }

        // 执行工具（强制 runId 贯穿审计与日志）
        String toolResult = executeApprovedTool(req.getToolKey(), req.getArgumentsSnapshotJson(), req.getRunId());

        // 将 toolResult 注入到 variables，供节点模板使用
        snap.variables.put("approvedToolResult", toolResult);
        snap.stepOutputs.put(req.getNodeKey() + ".approvedToolResult", toolResult);

        // 从 pending 节点重新执行（禁用工具），并继续后续图
        Graph graph = loadGraph(version.getId());
        validateGraph(graph);

        List<WorkflowNodeRun> nodeRuns = workflowNodeRunRepository.listByRunId(req.getRunId());
        if (nodeRuns == null) {
            nodeRuns = new ArrayList<>();
        }

        long start = System.currentTimeMillis();
        workflowRunRepository.updateStatus(req.getRunId(), "RUNNING", null, null);
        try {
            ExecutionResult exec = executeGraphFromNode(req.getRunId(), wf, version, graph, snap.sessionId, snap.content, snap.variables, snap.stepOutputs, nodeRuns, req.getNodeKey(), toolResult);
            long costMs = System.currentTimeMillis() - start;
            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(req.getRunId())
                    .status(exec.status)
                    .currentNodeKey(null)
                    .costMs(costMs)
                    .errorMessage(exec.status.equals("FAILED") ? exec.errorMessage : null)
                    .endedAt(LocalDateTime.now())
                    .build());
            workflowRunContextRepository.updateStatus(req.getRunId(), "RESUMED");
            return buildContractWithSteps(wf, version, req.getRunId(), costMs, exec.status, exec.contract, nodeRuns, null, null, null);
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(req.getRunId())
                    .status("FAILED")
                    .currentNodeKey(null)
                    .costMs(costMs)
                    .errorMessage(truncate(e.getMessage(), 1000))
                    .endedAt(LocalDateTime.now())
                    .build());
            try {
                workflowRunContextRepository.updateStatus(req.getRunId(), "EXPIRED");
            } catch (Exception ignore) {
            }
            PlatformContractV1 contract = PlatformContractV1.builder().answer("").uncertainty("").build();
            return buildContractWithSteps(wf, version, req.getRunId(), costMs, "FAILED", contract, nodeRuns, null, null, null);
        }
    }

    /**
     * 根据筛选条件查询工作流运行列表。
     *
     * @param status   状态值
     * @param offset   分页偏移量
     * @param pageSize 分页大小
     * @return 返回 WorkflowRun 分页数据。
     */
    @Override
    public PageResult<WorkflowRun> listRuns(String status, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        List<WorkflowRun> list = workflowRunRepository.list(status, safeOffset, safeSize);
        long total = workflowRunRepository.count(status);
        int pageNum = safeSize == 0 ? 1 : (safeOffset / safeSize) + 1;
        return PageResult.of(list, total, pageNum, safeSize);
    }

    /**
     * 查询工作流运行。
     *
     * @param runId 运行 ID
     * @return 返回 WorkflowRun 数据。
     */
    @Override
    public WorkflowRun getRun(String runId) {
        if (!StringUtils.hasText(runId)) {
            throw new IllegalArgumentException("runId 不能为空");
        }
        return workflowRunRepository.findByRunId(runId)
                .orElseThrow(() -> new NotFoundException("WorkflowRun 不存在，runId=" + runId));
    }

    /**
     * 根据筛选条件查询工作流运行列表。
     *
     * @param runId 运行 ID
     * @return 返回 WorkflowNodeRun 列表数据。
     */
    @Override
    public List<WorkflowNodeRun> listNodeRuns(String runId) {
        if (!StringUtils.hasText(runId)) {
            return Collections.emptyList();
        }
        return workflowNodeRunRepository.listByRunId(runId);
    }

    /**
     * 解析版本。
     *
     * @param wf 工作流定义。
     * @param workflowVersionId 工作流版本ID。
     * @return 返回WorkflowVersion对象。
     */
    private WorkflowVersion resolveVersion(Workflow wf, Long workflowVersionId) {
        if (workflowVersionId != null) {
            WorkflowVersion v = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().id(workflowVersionId).build())
                    .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
            if (v.getWorkflowId() == null || !v.getWorkflowId().equals(wf.getId())) {
                throw new BusinessException("WorkflowVersion 不属于当前 Workflow，workflowVersionId=" + workflowVersionId);
            }
            return v;
        }
        return workflowVersionRepository.findPublishedVersion(wf.getId())
                .orElseThrow(() -> new BusinessException("Workflow 未发布任何版本，code=" + wf.getWorkflowCode()));
    }

    private Graph loadGraph(Long workflowVersionId) {
        List<WorkflowNode> nodes = workflowGraphRepository.listNodes(WorkflowGraphQuery.builder()
                .workflowVersionId(workflowVersionId)
                .build());
        List<WorkflowEdge> edges = workflowGraphRepository.listEdges(WorkflowGraphQuery.builder()
                .workflowVersionId(workflowVersionId)
                .build());
        return new Graph(nodes, edges);
    }

    /**
     * 校验流程图。
     *
     * @param graph 流程图。
     */
    private void validateGraph(Graph graph) {
        if (graph == null || CollectionUtils.isEmpty(graph.nodes)) {
            throw new BusinessException("Workflow 图为空（缺少 nodes）");
        }
        String start = null;
        for (WorkflowNode n : graph.nodes) {
            if (n == null) {
                continue;
            }
            if ("START".equalsIgnoreCase(n.getNodeType())) {
                start = n.getNodeKey();
                break;
            }
        }
        if (!StringUtils.hasText(start)) {
            throw new BusinessException("Workflow 图缺少 START 节点");
        }
    }

    /**
     * 执行主流程并返回协议结果。
     *
     * @param runId 运行ID。
     * @param wf 工作流定义。
     * @param version 工作流版本。
     * @param graph 流程图。
     * @param sessionId 会话ID。
     * @param content 用户输入内容。
     * @param variables 运行变量。
     * @param stepOutputs 步骤输出映射。
     * @param nodeRuns 节点运行记录。
     * @return 返回ExecutionResult对象。
     */
    private ExecutionResult executeGraph(String runId,
                                         Workflow wf,
                                         WorkflowVersion version,
                                         Graph graph,
                                         Long sessionId,
                                         String content,
                                         Map<String, Object> variables,
                                         Map<String, Object> stepOutputs,
                                         List<WorkflowNodeRun> nodeRuns) {
        return executeGraphInternal(runId, wf, version, graph, sessionId, content, variables, stepOutputs, nodeRuns, null, null);
    }

    /**
     * 执行主流程并返回协议结果。
     *
     * @param runId 运行ID。
     * @param wf 工作流定义。
     * @param version 工作流版本。
     * @param graph 流程图。
     * @param sessionId 会话ID。
     * @param content 用户输入内容。
     * @param variables 运行变量。
     * @param stepOutputs 步骤输出映射。
     * @param nodeRuns 节点运行记录。
     * @param startFromNodeKey 起始节点Key。
     * @param approvedToolResult 审批后的工具结果。
     * @return 返回ExecutionResult对象。
     */
    private ExecutionResult executeGraphFromNode(String runId,
                                                 Workflow wf,
                                                 WorkflowVersion version,
                                                 Graph graph,
                                                 Long sessionId,
                                                 String content,
                                                 Map<String, Object> variables,
                                                 Map<String, Object> stepOutputs,
                                                 List<WorkflowNodeRun> nodeRuns,
                                                 String startFromNodeKey,
                                                 String approvedToolResult) {
        return executeGraphInternal(runId, wf, version, graph, sessionId, content, variables, stepOutputs, nodeRuns, startFromNodeKey, approvedToolResult);
    }

    /**
     * 执行主流程并返回协议结果。
     *
     * @param runId 运行ID。
     * @param wf 工作流定义。
     * @param version 工作流版本。
     * @param graph 流程图。
     * @param sessionId 会话ID。
     * @param content 用户输入内容。
     * @param variables 运行变量。
     * @param stepOutputs 步骤输出映射。
     * @param nodeRuns 节点运行记录。
     * @param startFromNodeKey 起始节点Key。
     * @param approvedToolResult 审批后的工具结果。
     * @return 返回ExecutionResult对象。
     */
    private ExecutionResult executeGraphInternal(String runId,
                                                 Workflow wf,
                                                 WorkflowVersion version,
                                                 Graph graph,
                                                 Long sessionId,
                                                 String content,
                                                 Map<String, Object> variables,
                                                 Map<String, Object> stepOutputs,
                                                 List<WorkflowNodeRun> nodeRuns,
                                                 String startFromNodeKey,
                                                 String approvedToolResult) {
        CallAdvisor[] workflowAdvisors = version == null || version.getId() == null
                ? new CallAdvisor[0]
                : agentEnhancerRuntimeService.resolveForWorkflowVersion(version.getId(), runId, sessionId);

        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        Map<String, List<WorkflowEdge>> out = new HashMap<>();
        for (WorkflowNode n : graph.nodes) {
            if (n == null || !StringUtils.hasText(n.getNodeKey())) {
                continue;
            }
            nodeMap.put(n.getNodeKey(), n);
        }
        if (graph.edges != null) {
            for (WorkflowEdge e : graph.edges) {
                if (e == null || !StringUtils.hasText(e.getSourceKey()) || !StringUtils.hasText(e.getTargetKey())) {
                    continue;
                }
                out.computeIfAbsent(e.getSourceKey(), k -> new ArrayList<>()).add(e);
            }
        }

        String startKey = null;
        for (WorkflowNode n : graph.nodes) {
            if (n != null && "START".equalsIgnoreCase(n.getNodeType())) {
                startKey = n.getNodeKey();
                break;
            }
        }
        if (!StringUtils.hasText(startKey)) {
            throw new BusinessException("Workflow 图缺少 START");
        }

        // 可达就绪队列：通过“激活边”累计 expectedPred，避免 join 被未激活分支卡住
        Set<String> completed = new HashSet<>();
        Set<String> reached = new HashSet<>();
        Map<String, Integer> expectedPred = new HashMap<>();
        Map<String, Integer> completedPred = new HashMap<>();
        Deque<String> ready = new ArrayDeque<>();

        String entry = StringUtils.hasText(startFromNodeKey) ? startFromNodeKey : startKey;
        reached.add(entry);
        expectedPred.put(entry, 0);
        completedPred.put(entry, 0);
        ready.add(entry);

        PlatformContractV1 finalContract = null;

        while (!ready.isEmpty()) {
            String nodeKey = ready.pollFirst();
            if (!reached.contains(nodeKey) || completed.contains(nodeKey)) {
                continue;
            }
            WorkflowNode node = nodeMap.get(nodeKey);
            if (node == null) {
                throw new BusinessException("节点不存在，nodeKey=" + nodeKey);
            }

            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(runId)
                    .status("RUNNING")
                    .currentNodeKey(nodeKey)
                    .costMs(null)
                    .errorMessage(null)
                    .endedAt(null)
                    .build());

            WorkflowNodeRun nodeRun = WorkflowNodeRun.builder()
                    .runId(runId)
                    .nodeKey(nodeKey)
                    .nodeType(node.getNodeType())
                    .nodeName(node.getNodeName())
                    .status("RUNNING")
                    .modelIdUsed(null)
                    .modelNameUsed(null)
                    .promptTokens(0)
                    .completionTokens(0)
                    .totalTokens(0)
                    .toolCallCount(0)
                    .toolDeniedCount(0)
                    .inputDigest(buildDigest(content))
                    .outputDigest(null)
                    .outputText(null)
                    .outputTruncated(0)
                    .approvalRequestId(null)
                    .costMs(null)
                    .errorMessage(null)
                    .startedAt(LocalDateTime.now())
                    .endedAt(null)
                    .build();

            // 若为续跑，从同一 runId 继续：避免重复插入（可多次执行），优先更新
            WorkflowNodeRun existed = workflowNodeRunRepository.findByRunIdAndNodeKey(runId, nodeKey).orElse(null);
            if (existed != null && existed.getId() != null && StringUtils.hasText(startFromNodeKey)) {
                nodeRun.setId(existed.getId());
                nodeRun.setStatus("RUNNING");
                nodeRun.setStartedAt(LocalDateTime.now());
                workflowNodeRunRepository.updateById(nodeRun);
            } else {
                workflowNodeRunRepository.insert(nodeRun);
                nodeRuns.add(nodeRun);
            }

            try {
                NodeResult result = executeNode(runId, wf, version, node, sessionId, content, variables, stepOutputs, workflowAdvisors,
                        StringUtils.hasText(startFromNodeKey) && nodeKey.equals(startFromNodeKey) ? approvedToolResult : null);

                completed.add(nodeKey);
                nodeRun.setStatus("SUCCESS");
                nodeRun.setCostMs(result.costMs);
                nodeRun.setEndedAt(LocalDateTime.now());
                nodeRun.setOutputTruncated(result.outputTruncated ? 1 : 0);
                nodeRun.setOutputText(result.outputText);
                nodeRun.setOutputDigest(buildDigest(result.outputText));
                nodeRun.setModelIdUsed(result.modelIdUsed);
                nodeRun.setModelNameUsed(result.modelNameUsed);
                nodeRun.setPromptTokens(result.promptTokens);
                nodeRun.setCompletionTokens(result.completionTokens);
                nodeRun.setTotalTokens(result.totalTokens);
                workflowNodeRunRepository.updateById(nodeRun);

                if (result.contract != null) {
                    finalContract = result.contract;
                }

                // 激活出边
                List<WorkflowEdge> edges = out.getOrDefault(nodeKey, Collections.emptyList());
                for (WorkflowEdge e : edges) {
                    if (!isEdgeEnabled(e, result.ifResult, variables, stepOutputs)) {
                        continue;
                    }
                    String target = e.getTargetKey();
                    reached.add(target);
                    expectedPred.put(target, expectedPred.getOrDefault(target, 0) + 1);
                    completedPred.put(target, completedPred.getOrDefault(target, 0) + 1);
                    if (completedPred.getOrDefault(target, 0).equals(expectedPred.getOrDefault(target, 0))) {
                        ready.add(target);
                    }
                }
            } catch (ApprovalRequiredException e) {
                nodeRun.setStatus("PENDING_APPROVAL");
                nodeRun.setApprovalRequestId(e.getApprovalRequestId());
                nodeRun.setErrorMessage(truncate(e.getMessage(), 1000));
                nodeRun.setEndedAt(LocalDateTime.now());
                workflowNodeRunRepository.updateById(nodeRun);

                return ExecutionResult.pending(finalContract, nodeKey, e.getApprovalRequestId(), e.getToolKey(), e.getRiskLevel());
            } catch (Exception e) {
                nodeRun.setStatus("FAILED");
                nodeRun.setErrorMessage(truncate(e.getMessage(), 1000));
                nodeRun.setEndedAt(LocalDateTime.now());
                workflowNodeRunRepository.updateById(nodeRun);
                return ExecutionResult.failed(finalContract, e.getMessage());
            }
        }

        if (finalContract == null) {
            // 无 OUTPUT 节点时兜底：把最后一个成功节点 output 当 answer
            String last = "";
            if (!nodeRuns.isEmpty()) {
                WorkflowNodeRun tail = nodeRuns.get(nodeRuns.size() - 1);
                last = tail == null || tail.getOutputText() == null ? "" : tail.getOutputText();
            }
            finalContract = PlatformContractV1.builder().answer(last).uncertainty("").build();
        }

        return ExecutionResult.success(finalContract);
    }

    /**
     * 执行主流程并返回协议结果。
     *
     * @param runId 运行ID。
     * @param wf 工作流定义。
     * @param version 工作流版本。
     * @param node 节点定义。
     * @param sessionId 会话ID。
     * @param content 用户输入内容。
     * @param variables 运行变量。
     * @param stepOutputs 步骤输出映射。
     * @param workflowAdvisors 工作流增强器数组。
     * @param approvedToolResult 审批后的工具结果。
     * @return 返回NodeResult对象。
     */
    private NodeResult executeNode(String runId,
                                   Workflow wf,
                                   WorkflowVersion version,
                                   WorkflowNode node,
                                   Long sessionId,
                                   String content,
                                   Map<String, Object> variables,
                                   Map<String, Object> stepOutputs,
                                   CallAdvisor[] workflowAdvisors,
                                   String approvedToolResult) {
        String type = node.getNodeType() == null ? "" : node.getNodeType().toUpperCase(Locale.ROOT);
        long start = System.currentTimeMillis();
        Map<String, Object> cfg = parseJsonMap(node.getConfigJson());
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("input", content);
        root.put("sessionId", sessionId);
        root.put("runId", runId);
        root.put("workflowCode", wf.getWorkflowCode());
        root.put("workflowId", wf.getId());
        root.put("workflowVersionId", version.getId());
        root.put("vars", variables == null ? Collections.emptyMap() : variables);
        root.put("steps", stepOutputs == null ? Collections.emptyMap() : stepOutputs);
        if (StringUtils.hasText(approvedToolResult)) {
            root.put("approvedToolResult", approvedToolResult);
        }
        WorkflowNodeExecutionContext context = new WorkflowNodeExecutionContext(
                type,
                start,
                runId,
                wf,
                version,
                node,
                sessionId,
                cfg,
                root,
                stepOutputs,
                workflowAdvisors,
                approvedToolResult
        );
        WorkflowNodeExecutor executor = nodeExecutors.get(type);
        if (executor == null) {
            throw new BusinessException("不支持的节点类型: " + type);
        }
        return executor.execute(context);
    }

    private NodeResult executePassThroughNode(WorkflowNodeExecutionContext context) {
        return NodeResult.text("", System.currentTimeMillis() - context.start);
    }

    private NodeResult executeRagRetrieveNode(WorkflowNodeExecutionContext context) {
        String queryTemplate = context.cfg.get("queryTemplate") == null
                ? "{{input}}"
                : String.valueOf(context.cfg.get("queryTemplate"));
        String query = renderTemplate(queryTemplate, context.root);
        List<String> ragTags = parseStringList(context.cfg.get("ragTags"));
        List<Document> docs = ragVectorStoreService.similaritySearch(query, ragTags);
        String ragText = formatDocs(docs);
        context.stepOutputs.put(context.node.getNodeKey() + ".ragText", ragText);
        context.stepOutputs.put(context.node.getNodeKey() + ".ragTags", ragTags);
        return NodeResult.text(ragText, System.currentTimeMillis() - context.start);
    }

    private NodeResult executeIfNode(WorkflowNodeExecutionContext context) {
        boolean ok = evaluateIf(context.cfg, context.root);
        context.stepOutputs.put(context.node.getNodeKey() + ".if", ok);
        return NodeResult.ifResult(ok, System.currentTimeMillis() - context.start);
    }

    private NodeResult executeToolCallNode(WorkflowNodeExecutionContext context) {
        String toolKey = context.cfg.get("toolKey") == null ? null : String.valueOf(context.cfg.get("toolKey"));
        if (!StringUtils.hasText(toolKey)) {
            throw new BusinessException("TOOL_CALL 缺少 toolKey，nodeKey=" + context.node.getNodeKey());
        }
        String argsTemplate = context.cfg.get("argumentsTemplateJson") == null
                ? "{}"
                : String.valueOf(context.cfg.get("argumentsTemplateJson"));
        String args = renderTemplate(argsTemplate, context.root);
        ToolCallback tool = findToolByKey(toolKey)
                .orElseThrow(() -> new NotFoundException("未找到工具回调，toolKey=" + toolKey));

        Set<String> allowedToolKeys = parseAllowedToolKeys(context.cfg.get("allowedToolKeysJson"));
        if (allowedToolKeys == null) {
            allowedToolKeys = Set.of(toolKey);
        }
        GatewayToolBindingContextHolder.setWorkflow(
                null,
                context.sessionId,
                context.wf.getId(),
                context.version.getId(),
                context.node.getNodeKey(),
                context.runId,
                allowedToolKeys
        );
        try {
            String result = tool.call(args);
            context.stepOutputs.put(context.node.getNodeKey() + ".toolKey", toolKey);
            context.stepOutputs.put(context.node.getNodeKey() + ".toolResult", result);
            return NodeResult.text(result, System.currentTimeMillis() - context.start);
        } finally {
            GatewayToolBindingContextHolder.clear();
        }
    }

    private NodeResult executeLlmOrOutputNode(WorkflowNodeExecutionContext context) {
        ModelConfig model = resolveModel(context.cfg);
        ChatClient chatClient = buildChatClient(model, context.cfg, context.workflowAdvisors);
        String system = context.cfg.get("systemPrompt") == null ? "" : String.valueOf(context.cfg.get("systemPrompt"));
        String userTemplate = context.cfg.get("userTemplate") == null ? "{{input}}" : String.valueOf(context.cfg.get("userTemplate"));
        String user = renderTemplate(userTemplate, context.root);

        List<SystemMessage> systemMessages = new ArrayList<>();
        if (StringUtils.hasText(system)) {
            systemMessages.add(new SystemMessage(system));
        }
        String ragFrom = context.cfg.get("ragFromNodeKey") == null ? null : String.valueOf(context.cfg.get("ragFromNodeKey"));
        if (StringUtils.hasText(ragFrom)) {
            Object ragTextObj = context.stepOutputs.get(ragFrom + ".ragText");
            if (ragTextObj != null) {
                String ragText = String.valueOf(ragTextObj);
                if (StringUtils.hasText(ragText)) {
                    systemMessages.add(new SystemMessage("你可以参考以下【参考文档】回答用户问题：\n" + ragText));
                }
            }
        }

        boolean outputContract = "OUTPUT".equals(context.type)
                || "CONTRACT".equalsIgnoreCase(String.valueOf(context.cfg.get("outputMode")));
        if (outputContract) {
            systemMessages.add(new SystemMessage(outputSupport.contractInstruction()));
        }
        if (StringUtils.hasText(context.approvedToolResult)) {
            systemMessages.add(new SystemMessage(
                    "已执行并通过审批的工具调用结果如下（仅供继续推理，不再触发工具调用）:\n"
                            + truncate(context.approvedToolResult, 8000)));
        }

        List<Message> messages = new ArrayList<>();
        messages.addAll(systemMessages);
        messages.add(new UserMessage(user));
        Prompt prompt = new Prompt(messages);

        Set<String> allowedToolKeys = parseAllowedToolKeys(context.cfg.get("allowedToolKeysJson"));
        Long modelId = model == null ? null : model.getId();
        GatewayToolBindingContextHolder.setWorkflow(
                modelId,
                context.sessionId,
                context.wf.getId(),
                context.version.getId(),
                context.node.getNodeKey(),
                context.runId,
                allowedToolKeys
        );
        try {
            ChatResponse resp = chatClient.prompt(prompt).call().chatResponse();
            String raw = extractText(resp);
            if (!outputContract) {
                String safe = sanitizeAndLimit(raw).text;
                context.stepOutputs.put(context.node.getNodeKey() + ".text", safe);
                NodeResult result = NodeResult.text(safe, System.currentTimeMillis() - context.start);
                result.modelIdUsed = model == null ? null : model.getId();
                result.modelNameUsed = model == null ? null : model.getModelName();
                applyUsage(result, resp);
                return result;
            }

            PlatformContractV1 parsed = outputSupport.parseOrNull(raw);
            if (parsed == null) {
                parsed = repairOnce(chatClient, raw);
            }
            if (parsed == null) {
                throw new BusinessException("OUTPUT 节点输出无法解析为 PlatformContractV1");
            }
            context.stepOutputs.put(context.node.getNodeKey() + ".contract.answer", parsed.getAnswer());
            NodeResult result = NodeResult.contract(parsed, sanitizeAndLimit(raw).text, System.currentTimeMillis() - context.start);
            result.modelIdUsed = model == null ? null : model.getId();
            result.modelNameUsed = model == null ? null : model.getModelName();
            applyUsage(result, resp);
            return result;
        } finally {
            GatewayToolBindingContextHolder.clear();
        }
    }

    private void applyUsage(NodeResult r, ChatResponse resp) {
        if (r == null || resp == null || resp.getMetadata() == null || resp.getMetadata().getUsage() == null) {
            return;
        }
        try {
            Usage u = resp.getMetadata().getUsage();
            if (u == null) {
                return;
            }
            r.promptTokens = u.getPromptTokens() == null ? 0 : u.getPromptTokens().intValue();
            r.completionTokens = u.getCompletionTokens() == null ? 0 : u.getCompletionTokens().intValue();
            r.totalTokens = u.getTotalTokens() == null ? 0 : u.getTotalTokens().intValue();
        } catch (Exception ignore) {
        }
    }

    /**
     * 判断边条件是否满足并允许流转。
     *
     * @param edge 流程连线。
     * @param ifResult 条件节点计算结果。
     * @param variables 运行变量。
     * @param stepOutputs 步骤输出映射。
     * @return 返回是否满足边的启用条件。
     */
    private boolean isEdgeEnabled(WorkflowEdge edge, Boolean ifResult, Map<String, Object> variables, Map<String, Object> stepOutputs) {
        if (edge == null) {
            return false;
        }
        String type = edge.getEdgeType() == null ? "DEFAULT" : edge.getEdgeType().toUpperCase(Locale.ROOT);
        if ("DEFAULT".equals(type)) {
            return true;
        }
        if ("TRUE".equals(type)) {
            return Boolean.TRUE.equals(ifResult);
        }
        if ("FALSE".equals(type)) {
            return Boolean.FALSE.equals(ifResult);
        }
        if ("CONDITION".equals(type)) {
            // 最小实现：支持 ${vars.xxx} / ${steps.xxx} 的存在性/等值判断（非常轻量）
            String expr = edge.getConditionExpr();
            if (!StringUtils.hasText(expr)) {
                return false;
            }
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("vars", variables == null ? Collections.emptyMap() : variables);
            root.put("steps", stepOutputs == null ? Collections.emptyMap() : stepOutputs);
            return evaluateConditionExpr(expr, root);
        }
        return true;
    }

    private boolean evaluateIf(Map<String, Object> cfg, Map<String, Object> root) {
        // 支持 config: { "varPath":"vars.someKey", "equals":"x" }
        Object varPath = cfg.get("varPath");
        if (varPath == null) {
            // fallback: { "expr":"..." }
            Object expr = cfg.get("expr");
            if (expr != null) {
                return evaluateConditionExpr(String.valueOf(expr), root);
            }
            return false;
        }
        Object val = getByPath(root, String.valueOf(varPath));
        Object equals = cfg.get("equals");
        if (equals == null) {
            return val != null && StringUtils.hasText(String.valueOf(val));
        }
        return String.valueOf(equals).equals(String.valueOf(val));
    }

    private boolean evaluateConditionExpr(String expr, Map<String, Object> root) {
        String e = expr.trim();
        // 仅支持：{{path}} == 'x' / != / exists({{path}})
        if (e.startsWith("exists(") && e.endsWith(")")) {
            String inner = e.substring("exists(".length(), e.length() - 1).trim();
            String path = stripBraces(inner);
            Object v = getByPath(root, path);
            return v != null && StringUtils.hasText(String.valueOf(v));
        }
        String[] ops = new String[]{"==", "!="};
        for (String op : ops) {
            int idx = e.indexOf(op);
            if (idx > 0) {
                String left = e.substring(0, idx).trim();
                String right = e.substring(idx + op.length()).trim();
                String path = stripBraces(left);
                Object v = getByPath(root, path);
                String rv = stripQuotes(right);
                boolean eq = String.valueOf(v).equals(rv);
                return "==".equals(op) ? eq : !eq;
            }
        }
        // fallback: truthy path
        String path = stripBraces(e);
        Object v = getByPath(root, path);
        return v != null && StringUtils.hasText(String.valueOf(v));
    }

    private String stripBraces(String s) {
        String t = s.trim();
        if (t.startsWith("{{") && t.endsWith("}}")) {
            return t.substring(2, t.length() - 2).trim();
        }
        return t;
    }

    private String stripQuotes(String s) {
        String t = s.trim();
        if ((t.startsWith("'") && t.endsWith("'")) || (t.startsWith("\"") && t.endsWith("\""))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    private ChatClient buildChatClient(ModelConfig model,
                                       Map<String, Object> cfg,
                                       CallAdvisor[] workflowAdvisors) {
        if (model == null) {
            throw new BusinessException("未找到可用模型");
        }
        Boolean toolEnabled = cfg.get("toolEnabled") == null ? null : Boolean.valueOf(String.valueOf(cfg.get("toolEnabled")));
        boolean enableTools = toolEnabled == null || toolEnabled;
        return chatClientAssemblyService.buildChatClient(model, enableTools, workflowAdvisors == null ? new CallAdvisor[0] : workflowAdvisors);
    }

    private ModelConfig resolveModel(Map<String, Object> cfg) {
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

    private Optional<ToolCallback> findToolByKey(String toolKey) {
        if (toolCallbackProvider == null) {
            return Optional.empty();
        }
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        if (callbacks == null || callbacks.length == 0) {
            return Optional.empty();
        }
        for (ToolCallback cb : callbacks) {
            if (cb instanceof ToolKeyAware aware) {
                if (toolKey.equals(aware.toolKey())) {
                    return Optional.of(cb);
                }
            }
        }
        return Optional.empty();
    }

    private String executeApprovedTool(String toolKey, String argumentsSnapshotJson, String runId) {
        ToolCallback tool = findToolByKey(toolKey)
                .orElseThrow(() -> new NotFoundException("未找到目标工具回调，toolKey=" + toolKey));

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

    private PlatformContractV1 repairOnce(ChatClient client, String invalidOutput) {
        if (client == null) {
            return null;
        }
        String safe = invalidOutput == null ? "" : invalidOutput;
        Prompt prompt = new Prompt(
                new SystemMessage("你是 JSON 修复器。你必须仅输出合法 JSON，不要输出任何额外文字。"),
                new SystemMessage(outputSupport.contractInstruction()),
                new UserMessage("请将以下内容修复为符合要求的 JSON：\n" + safe)
        );
        ChatResponse resp = client.prompt(prompt).call().chatResponse();
        if (resp == null) {
            return null;
        }
        String raw = extractText(resp);
        return outputSupport.parseOrNull(raw);
    }

    private String extractText(ChatResponse resp) {
        if (resp == null || resp.getResult() == null || resp.getResult().getOutput() == null) {
            return "";
        }
        String text = resp.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private String formatDocs(List<Document> docs) {
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
            String text = d.getText() == null ? "" : d.getText();
            if (text.length() > 1600) {
                text = text.substring(0, 1600);
            }
            sb.append(text).append('\n').append('\n');
            if (idx >= 5) {
                break;
            }
        }
        return sb.toString();
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return JsonMapUtils.readMap(objectMapper, json);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private List<String> parseStringList(Object v) {
        if (v == null) {
            return Collections.emptyList();
        }
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String s = String.valueOf(o);
                if (StringUtils.hasText(s)) {
                    out.add(s);
                }
            }
            return out;
        }
        String s = String.valueOf(v);
        if (!StringUtils.hasText(s)) {
            return Collections.emptyList();
        }
        // 尝试按 JSON 数组解析
        try {
            List<String> list = objectMapper.readValue(s, new TypeReference<List<String>>() {
            });
            return list == null ? Collections.emptyList() : list;
        } catch (Exception ignore) {
        }
        // 逗号分割兜底
        String[] arr = s.split(",");
        List<String> out = new ArrayList<>();
        for (String it : arr) {
            if (StringUtils.hasText(it)) {
                out.add(it.trim());
            }
        }
        return out;
    }

    private Set<String> parseAllowedToolKeys(Object jsonOrList) {
        if (jsonOrList == null) {
            return null;
        }
        if (jsonOrList instanceof List<?> list) {
            Set<String> set = new HashSet<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String s = String.valueOf(o);
                if (StringUtils.hasText(s)) {
                    set.add(s);
                }
            }
            return set;
        }
        String json = String.valueOf(jsonOrList);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            if (list == null) {
                return null;
            }
            return new HashSet<>(list);
        } catch (Exception e) {
            return null;
        }
    }

    private String renderTemplate(String template, Map<String, Object> root) {
        if (!StringUtils.hasText(template)) {
            return "";
        }
        String t = template;
        int guard = 0;
        while (guard++ < 50) {
            int s = t.indexOf("{{");
            int e = t.indexOf("}}", s + 2);
            if (s < 0 || e < 0) {
                break;
            }
            String key = t.substring(s + 2, e).trim();
            Object val = getByPath(root, key);
            String rep = val == null ? "" : String.valueOf(val);
            t = t.substring(0, s) + rep + t.substring(e + 2);
        }
        return t;
    }

    private Object getByPath(Map<String, Object> root, String path) {
        if (root == null || !StringUtils.hasText(path)) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object cur = root;
        for (String p : parts) {
            if (cur == null) {
                return null;
            }
            if (cur instanceof Map<?, ?> m) {
                cur = m.get(p);
                continue;
            }
            return null;
        }
        return cur;
    }

    private DigestText sanitizeAndLimit(String raw) {
        String s = raw == null ? "" : raw;
        s = s.replaceAll("sk-[A-Za-z0-9]{10,}", "sk-***");
        s = s.replaceAll("Bearer\\s+[A-Za-z0-9\\-\\._~\\+\\/]+=*", "Bearer ***");
        boolean truncated = false;
        if (s.length() > MAX_OUTPUT_TEXT_CHARS) {
            s = s.substring(0, MAX_OUTPUT_TEXT_CHARS);
            truncated = true;
        }
        return new DigestText(s, truncated);
    }

    private String buildDigest(String raw) {
        String s = raw == null ? "" : raw;
        DigestText dt = sanitizeAndLimit(s);
        String t = dt.text;
        if (t.length() > MAX_DIGEST_CHARS) {
            t = t.substring(0, MAX_DIGEST_CHARS);
        }
        return t;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private void saveRunContextSnapshot(String runId,
                                        Workflow wf,
                                        WorkflowVersion version,
                                        Long sessionId,
                                        String content,
                                        Map<String, Object> variables,
                                        Map<String, Object> stepOutputs,
                                        String pendingNodeKey,
                                        Long approvalRequestId,
                                        String pendingToolKey,
                                        String pendingRiskLevel) {
        if (workflowRunContextRepository == null) {
            return;
        }
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("workflowId", wf == null ? null : wf.getId());
            map.put("workflowCode", wf == null ? null : wf.getWorkflowCode());
            map.put("workflowVersionId", version == null ? null : version.getId());
            map.put("sessionId", sessionId);
            map.put("content", content);
            map.put("variables", variables == null ? Collections.emptyMap() : variables);
            map.put("stepOutputs", stepOutputs == null ? Collections.emptyMap() : stepOutputs);
            map.put("pendingNodeKey", pendingNodeKey);
            map.put("approvalRequestId", approvalRequestId);
            map.put("pendingToolKey", pendingToolKey);
            map.put("pendingRiskLevel", pendingRiskLevel);
            map.put("savedAt", LocalDateTime.now().toString());
            String json = objectMapper.writeValueAsString(map);

            workflowRunContextRepository.upsert(WorkflowRunContext.builder()
                    .runId(runId)
                    .status("SAVED")
                    .snapshotJson(json)
                    .build());
        } catch (Exception e) {
            log.warn("保存 workflow_run_context 快照失败，runId: {}", runId, e);
        }
    }

    private WorkflowSnapshot loadSnapshot(String runId) {
        if (workflowRunContextRepository == null || !StringUtils.hasText(runId)) {
            return null;
        }
        return workflowRunContextRepository.findByRunId(runId)
                .map(ctx -> {
                    if (ctx == null || !StringUtils.hasText(ctx.getSnapshotJson())) {
                        return null;
                    }
                    try {
                        Map<String, Object> map = JsonMapUtils.readMap(objectMapper, ctx.getSnapshotJson());
                        Long sessionId = map.get("sessionId") == null ? null : Long.valueOf(String.valueOf(map.get("sessionId")));
                        String content = map.get("content") == null ? "" : String.valueOf(map.get("content"));
                        Map<String, Object> vars = map.get("variables") instanceof Map<?, ?>
                                ? JsonMapUtils.convertToMap(objectMapper, map.get("variables"))
                                : new LinkedHashMap<>();
                        Map<String, Object> steps = map.get("stepOutputs") instanceof Map<?, ?>
                                ? JsonMapUtils.convertToMap(objectMapper, map.get("stepOutputs"))
                                : new LinkedHashMap<>();
                        String pendingNodeKey = map.get("pendingNodeKey") == null ? null : String.valueOf(map.get("pendingNodeKey"));
                        return new WorkflowSnapshot(sessionId, content, vars, steps, pendingNodeKey);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    private PlatformContractV1 buildContractWithSteps(Workflow wf,
                                                      WorkflowVersion version,
                                                      String runId,
                                                      long costMs,
                                                      String status,
                                                      PlatformContractV1 contract,
                                                      List<WorkflowNodeRun> nodeRuns,
                                                      Long approvalRequestId,
                                                      String pendingToolKey,
                                                      String riskLevel) {
        PlatformContractV1 base = contract == null ? PlatformContractV1.builder().answer("").uncertainty("").build() : contract;
        List<PlatformContractV1.StepTrace> steps = new ArrayList<>();
        if (nodeRuns != null) {
            for (WorkflowNodeRun nr : nodeRuns) {
                if (nr == null) {
                    continue;
                }
                steps.add(PlatformContractV1.StepTrace.builder()
                        .nodeKey(nr.getNodeKey())
                        .nodeType(nr.getNodeType())
                        .nodeName(nr.getNodeName())
                        .status(nr.getStatus())
                        .costMs(nr.getCostMs())
                        .toolCallCount(nr.getToolCallCount())
                        .toolDeniedCount(nr.getToolDeniedCount())
                        .inputDigest(nr.getInputDigest())
                        .outputDigest(nr.getOutputDigest())
                        .outputText(nr.getOutputText())
                        .outputTruncated(nr.getOutputTruncated() != null && nr.getOutputTruncated() == 1)
                        .approvalRequestId(nr.getApprovalRequestId())
                        .errorMessage(nr.getErrorMessage())
                        .build());
            }
        }

        PlatformContractV1.Meta meta = PlatformContractV1.Meta.builder()
                .runId(runId)
                .agentCode(null)
                .agentVersionId(null)
                .agentVersionNo(null)
                .modelUsed(null)
                .costMs(costMs)
                .repairAttempts(0)
                .workflowId(wf == null ? null : wf.getId())
                .workflowCode(wf == null ? null : wf.getWorkflowCode())
                .workflowVersionId(version == null ? null : version.getId())
                .workflowVersionNo(version == null ? null : version.getVersionNo())
                .approvalRequestId(approvalRequestId)
                .pendingToolKey(pendingToolKey)
                .riskLevel(riskLevel)
                .build();

        return PlatformContractV1.builder()
                .meta(meta)
                .answer(base.getAnswer())
                .uncertainty(base.getUncertainty())
                .citations(base.getCitations())
                .toolCalls(base.getToolCalls())
                .actionsNext(base.getActionsNext())
                .steps(steps)
                .status(status)
                .error(base.getError())
                .build();
    }

    @FunctionalInterface
    private interface WorkflowNodeExecutor {
        NodeResult execute(WorkflowNodeExecutionContext context);
    }

    private static final class WorkflowNodeExecutionContext {
        private final String type;
        private final long start;
        private final String runId;
        private final Workflow wf;
        private final WorkflowVersion version;
        private final WorkflowNode node;
        private final Long sessionId;
        private final Map<String, Object> cfg;
        private final Map<String, Object> root;
        private final Map<String, Object> stepOutputs;
        private final CallAdvisor[] workflowAdvisors;
        private final String approvedToolResult;

        private WorkflowNodeExecutionContext(String type,
                                             long start,
                                             String runId,
                                             Workflow wf,
                                             WorkflowVersion version,
                                             WorkflowNode node,
                                             Long sessionId,
                                             Map<String, Object> cfg,
                                             Map<String, Object> root,
                                             Map<String, Object> stepOutputs,
                                             CallAdvisor[] workflowAdvisors,
                                             String approvedToolResult) {
            this.type = type;
            this.start = start;
            this.runId = runId;
            this.wf = wf;
            this.version = version;
            this.node = node;
            this.sessionId = sessionId;
            this.cfg = cfg;
            this.root = root;
            this.stepOutputs = stepOutputs;
            this.workflowAdvisors = workflowAdvisors;
            this.approvedToolResult = approvedToolResult;
        }
    }

    private static final class Graph {
        private final List<WorkflowNode> nodes;
        private final List<WorkflowEdge> edges;

        private Graph(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
            this.nodes = nodes == null ? Collections.emptyList() : nodes;
            this.edges = edges == null ? Collections.emptyList() : edges;
        }
    }

    private static final class ExecutionResult {
        private final String status;
        private final PlatformContractV1 contract;
        private final String errorMessage;
        private final String pendingNodeKey;
        private final Long approvalRequestId;
        private final String pendingToolKey;
        private final String pendingRiskLevel;

        private ExecutionResult(String status,
                                PlatformContractV1 contract,
                                String errorMessage,
                                String pendingNodeKey,
                                Long approvalRequestId,
                                String pendingToolKey,
                                String pendingRiskLevel) {
            this.status = status;
            this.contract = contract;
            this.errorMessage = errorMessage;
            this.pendingNodeKey = pendingNodeKey;
            this.approvalRequestId = approvalRequestId;
            this.pendingToolKey = pendingToolKey;
            this.pendingRiskLevel = pendingRiskLevel;
        }

        static ExecutionResult success(PlatformContractV1 contract) {
            return new ExecutionResult("SUCCESS", contract, null, null, null, null, null);
        }

        static ExecutionResult failed(PlatformContractV1 contract, String error) {
            return new ExecutionResult("FAILED", contract, error, null, null, null, null);
        }

        static ExecutionResult pending(PlatformContractV1 contract, String pendingNodeKey, Long approvalId, String toolKey, String riskLevel) {
            return new ExecutionResult("PENDING_APPROVAL", contract, null, pendingNodeKey, approvalId, toolKey, riskLevel);
        }
    }

    private static final class NodeResult {
        private String outputText;
        private boolean outputTruncated;
        private long costMs;
        private PlatformContractV1 contract;
        private Boolean ifResult;

        private Long modelIdUsed;
        private String modelNameUsed;
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;

        static NodeResult text(String text, long costMs) {
            NodeResult r = new NodeResult();
            r.outputText = text == null ? "" : text;
            r.costMs = costMs;
            return r;
        }

        static NodeResult contract(PlatformContractV1 contract, String rawText, long costMs) {
            NodeResult r = new NodeResult();
            r.contract = contract;
            r.outputText = rawText == null ? "" : rawText;
            r.costMs = costMs;
            return r;
        }

        static NodeResult ifResult(boolean ok, long costMs) {
            NodeResult r = new NodeResult();
            r.ifResult = ok;
            r.outputText = String.valueOf(ok);
            r.costMs = costMs;
            return r;
        }
    }

    private static final class DigestText {
        private final String text;
        private final boolean truncated;

        private DigestText(String text, boolean truncated) {
            this.text = text;
            this.truncated = truncated;
        }
    }

    private static final class WorkflowSnapshot {
        private Long sessionId;
        private String content;
        private Map<String, Object> variables;
        private Map<String, Object> stepOutputs;
        private String pendingNodeKey;

        private WorkflowSnapshot(Long sessionId, String content, Map<String, Object> variables, Map<String, Object> stepOutputs, String pendingNodeKey) {
            this.sessionId = sessionId;
            this.content = content;
            this.variables = variables == null ? new LinkedHashMap<>() : variables;
            this.stepOutputs = stepOutputs == null ? new LinkedHashMap<>() : stepOutputs;
            this.pendingNodeKey = pendingNodeKey;
        }
    }
}
