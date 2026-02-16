package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.WorkflowRuntimeAppService;
import com.xbk.knowledge.application.service.runtime.AdvisorRuntimeService;
import com.xbk.knowledge.application.support.contract.PlatformContractV1OutputSupport;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;
import com.xbk.knowledge.domain.model.entity.workflow.Workflow;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowEdge;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNode;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNodeRun;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRun;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRunContext;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowVersion;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowCodeQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowGraphQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.repository.approval.ApprovalRequestRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowGraphRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowNodeRunRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRunContextRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRunRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowVersionRepository;
import com.xbk.knowledge.domain.service.model.IModelConfigService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.ApprovalRequiredException;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.tool.ToolInvokeBypassContextHolder;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Workflow 运行面应用服务实现（DAG：当前实现为“可达节点的拓扑就绪队列”，并支持条件边）。
 
  * @author xiexu
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
    private final AdvisorRuntimeService advisorRuntimeService;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;
    private final PlatformContractV1OutputSupport outputSupport;
    private final IdentityContextService identityContextService;
    private final RagVectorStoreService ragVectorStoreService;

    @Override
    public PlatformContractV1 run(Long orgId,
                                  String workflowCode,
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

        Workflow wf = workflowRepository.findByCode(WorkflowCodeQuery.builder().orgId(orgId).workflowCode(workflowCode).build())
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，code=" + workflowCode));
        if (!"ENABLED".equalsIgnoreCase(wf.getStatus())) {
            throw new BusinessException("Workflow 未启用，code=" + workflowCode);
        }

        WorkflowVersion version = resolveVersion(orgId, wf, workflowVersionId);
        Graph graph = loadGraph(orgId, version.getId());
        validateGraph(graph);

        OrgContext ctx = OrgContextHolder.get();
        Long operatorId = ctx == null ? null : ctx.operatorUserId();
        String operatorType = operatorId == null ? "system" : "user";

        WorkflowRun run = WorkflowRun.builder()
                .runId(runId)
                .orgId(orgId)
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
            ExecutionResult exec = executeGraph(orgId, runId, wf, version, graph, sessionId, content, variables, stepOutputs, nodeRuns);
            long costMs = System.currentTimeMillis() - start;

            if (exec.status.equals("PENDING_APPROVAL")) {
                workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                        .runId(runId)
                        .orgId(orgId)
                        .status("PENDING_APPROVAL")
                        .currentNodeKey(exec.pendingNodeKey)
                        .costMs(costMs)
                        .errorMessage(null)
                        .endedAt(LocalDateTime.now())
                        .build());

                saveRunContextSnapshot(orgId, runId, wf, version, sessionId, content, variables, stepOutputs, exec.pendingNodeKey, exec.approvalRequestId, exec.pendingToolKey, exec.pendingRiskLevel);

                return buildContractWithSteps(orgId, wf, version, runId, costMs, "PENDING_APPROVAL", exec.contract, nodeRuns, exec.approvalRequestId, exec.pendingToolKey, exec.pendingRiskLevel);
            }

            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(runId)
                    .orgId(orgId)
                    .status(exec.status)
                    .currentNodeKey(null)
                    .costMs(costMs)
                    .errorMessage(exec.status.equals("FAILED") ? exec.errorMessage : null)
                    .endedAt(LocalDateTime.now())
                    .build());

            return buildContractWithSteps(orgId, wf, version, runId, costMs, exec.status, exec.contract, nodeRuns, null, null, null);
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(runId)
                    .orgId(orgId)
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
            return buildContractWithSteps(orgId, wf, version, runId, costMs, "FAILED", contract, nodeRuns, null, null, null);
        }
    }

    /**
     * resumeFromApproval。
     *
     * @param orgId 参数
     * @param approvalRequestId 参数
     * @return 返回结果
     */
    @Override
    public PlatformContractV1 resumeFromApproval(Long orgId, Long approvalRequestId) {
        if (orgId == null || approvalRequestId == null) {
            throw new IllegalArgumentException("orgId/approvalRequestId 不能为空");
        }
        ApprovalRequest req = approvalRequestRepository.findById(orgId, approvalRequestId)
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

        Workflow wf = workflowRepository.findById(new IdQuery(orgId, req.getWorkflowId()))
                .orElseThrow(() -> new NotFoundException("Workflow 不存在，id=" + req.getWorkflowId()));
        WorkflowVersion version = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().orgId(orgId).id(req.getWorkflowVersionId()).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + req.getWorkflowVersionId()));

        // 读取上下文快照
        WorkflowSnapshot snap = loadSnapshot(orgId, req.getRunId());
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
        Graph graph = loadGraph(orgId, version.getId());
        validateGraph(graph);

        List<WorkflowNodeRun> nodeRuns = workflowNodeRunRepository.listByRunId(orgId, req.getRunId());
        if (nodeRuns == null) {
            nodeRuns = new ArrayList<>();
        }

        long start = System.currentTimeMillis();
        workflowRunRepository.updateStatus(orgId, req.getRunId(), "RUNNING", null, null);
        try {
            ExecutionResult exec = executeGraphFromNode(orgId, req.getRunId(), wf, version, graph, snap.sessionId, snap.content, snap.variables, snap.stepOutputs, nodeRuns, req.getNodeKey(), toolResult);
            long costMs = System.currentTimeMillis() - start;
            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(req.getRunId())
                    .orgId(orgId)
                    .status(exec.status)
                    .currentNodeKey(null)
                    .costMs(costMs)
                    .errorMessage(exec.status.equals("FAILED") ? exec.errorMessage : null)
                    .endedAt(LocalDateTime.now())
                    .build());
            workflowRunContextRepository.updateStatus(orgId, req.getRunId(), "RESUMED");
            return buildContractWithSteps(orgId, wf, version, req.getRunId(), costMs, exec.status, exec.contract, nodeRuns, null, null, null);
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            workflowRunRepository.updateStatusAndMetrics(WorkflowRun.builder()
                    .runId(req.getRunId())
                    .orgId(orgId)
                    .status("FAILED")
                    .currentNodeKey(null)
                    .costMs(costMs)
                    .errorMessage(truncate(e.getMessage(), 1000))
                    .endedAt(LocalDateTime.now())
                    .build());
            try {
                workflowRunContextRepository.updateStatus(orgId, req.getRunId(), "EXPIRED");
            } catch (Exception ignore) {
            }
            PlatformContractV1 contract = PlatformContractV1.builder().answer("").uncertainty("").build();
            return buildContractWithSteps(orgId, wf, version, req.getRunId(), costMs, "FAILED", contract, nodeRuns, null, null, null);
        }
    }

    /**
     * listRuns。
     *
     * @param orgId 参数
     * @param status 参数
     * @param offset 参数
     * @param pageSize 参数
     * @return 返回结果
     */
    @Override
    public PageResult<WorkflowRun> listRuns(Long orgId, String status, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        List<WorkflowRun> list = workflowRunRepository.list(orgId, status, safeOffset, safeSize);
        long total = workflowRunRepository.count(orgId, status);
        int pageNum = safeSize == 0 ? 1 : (safeOffset / safeSize) + 1;
        return PageResult.of(list, total, pageNum, safeSize);
    }

    /**
     * getRun。
     *
     * @param orgId 参数
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public WorkflowRun getRun(Long orgId, String runId) {
        if (orgId == null || !StringUtils.hasText(runId)) {
            throw new IllegalArgumentException("orgId/runId 不能为空");
        }
        return workflowRunRepository.findByRunId(orgId, runId)
                .orElseThrow(() -> new NotFoundException("WorkflowRun 不存在，runId=" + runId));
    }

    /**
     * listNodeRuns。
     *
     * @param orgId 参数
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowNodeRun> listNodeRuns(Long orgId, String runId) {
        if (!StringUtils.hasText(runId)) {
            return Collections.emptyList();
        }
        return workflowNodeRunRepository.listByRunId(orgId, runId);
    }

    private WorkflowVersion resolveVersion(Long orgId, Workflow wf, Long workflowVersionId) {
        if (workflowVersionId != null) {
            WorkflowVersion v = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().orgId(orgId).id(workflowVersionId).build())
                    .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));
            if (v.getWorkflowId() == null || !v.getWorkflowId().equals(wf.getId())) {
                throw new BusinessException("WorkflowVersion 不属于当前 Workflow，workflowVersionId=" + workflowVersionId);
            }
            return v;
        }
        return workflowVersionRepository.findPublishedVersion(orgId, wf.getId())
                .orElseThrow(() -> new BusinessException("Workflow 未发布任何版本，code=" + wf.getWorkflowCode()));
    }

    private Graph loadGraph(Long orgId, Long workflowVersionId) {
        List<WorkflowNode> nodes = workflowGraphRepository.listNodes(WorkflowGraphQuery.builder()
                .orgId(orgId)
                .workflowVersionId(workflowVersionId)
                .build());
        List<WorkflowEdge> edges = workflowGraphRepository.listEdges(WorkflowGraphQuery.builder()
                .orgId(orgId)
                .workflowVersionId(workflowVersionId)
                .build());
        return new Graph(nodes, edges);
    }

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

    private ExecutionResult executeGraph(Long orgId,
                                        String runId,
                                        Workflow wf,
                                        WorkflowVersion version,
                                        Graph graph,
                                        Long sessionId,
                                        String content,
                                        Map<String, Object> variables,
                                        Map<String, Object> stepOutputs,
                                        List<WorkflowNodeRun> nodeRuns) {
        return executeGraphInternal(orgId, runId, wf, version, graph, sessionId, content, variables, stepOutputs, nodeRuns, null, null);
    }

    private ExecutionResult executeGraphFromNode(Long orgId,
                                                String runId,
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
        return executeGraphInternal(orgId, runId, wf, version, graph, sessionId, content, variables, stepOutputs, nodeRuns, startFromNodeKey, approvedToolResult);
    }

    private ExecutionResult executeGraphInternal(Long orgId,
                                                String runId,
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
        org.springframework.ai.chat.client.advisor.api.CallAdvisor[] workflowAdvisors = version == null || version.getId() == null
                ? new org.springframework.ai.chat.client.advisor.api.CallAdvisor[0]
                : advisorRuntimeService.resolveForWorkflowVersion(orgId, version.getId(), runId, sessionId);

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
        Set<String> completed = new java.util.HashSet<>();
        Set<String> reached = new java.util.HashSet<>();
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
                    .orgId(orgId)
                    .status("RUNNING")
                    .currentNodeKey(nodeKey)
                    .costMs(null)
                    .errorMessage(null)
                    .endedAt(null)
                    .build());

            WorkflowNodeRun nodeRun = WorkflowNodeRun.builder()
                    .orgId(orgId)
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
            WorkflowNodeRun existed = workflowNodeRunRepository.findByRunIdAndNodeKey(orgId, runId, nodeKey).orElse(null);
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
                NodeResult result = executeNode(orgId, runId, wf, version, node, sessionId, content, variables, stepOutputs, workflowAdvisors,
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

    private NodeResult executeNode(Long orgId,
                                  String runId,
                                  Workflow wf,
                                  WorkflowVersion version,
                                  WorkflowNode node,
                                  Long sessionId,
                                  String content,
                                  Map<String, Object> variables,
                                  Map<String, Object> stepOutputs,
                                  org.springframework.ai.chat.client.advisor.api.CallAdvisor[] workflowAdvisors,
                                  String approvedToolResult) {
        String type = node.getNodeType() == null ? "" : node.getNodeType().toUpperCase(Locale.ROOT);
        long start = System.currentTimeMillis();

        Map<String, Object> cfg = parseJsonMap(node.getConfigJson());

        // 统一可用变量
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

        if ("START".equals(type) || "PARALLEL".equals(type) || "JOIN".equals(type) || "END".equals(type)) {
            return NodeResult.text("", System.currentTimeMillis() - start);
        }

        if ("RAG_RETRIEVE".equals(type)) {
            String queryTemplate = cfg.get("queryTemplate") == null ? "{{input}}" : String.valueOf(cfg.get("queryTemplate"));
            String query = renderTemplate(queryTemplate, root);
            List<String> ragTags = parseStringList(cfg.get("ragTags"));
            List<Document> docs = ragVectorStoreService.similaritySearch(query, ragTags);
            String ragText = formatDocs(docs);
            stepOutputs.put(node.getNodeKey() + ".ragText", ragText);
            stepOutputs.put(node.getNodeKey() + ".ragTags", ragTags);
            return NodeResult.text(ragText, System.currentTimeMillis() - start);
        }

        if ("IF".equals(type)) {
            boolean ok = evaluateIf(cfg, root);
            stepOutputs.put(node.getNodeKey() + ".if", ok);
            return NodeResult.ifResult(ok, System.currentTimeMillis() - start);
        }

        if ("TOOL_CALL".equals(type)) {
            String toolKey = cfg.get("toolKey") == null ? null : String.valueOf(cfg.get("toolKey"));
            if (!StringUtils.hasText(toolKey)) {
                throw new BusinessException("TOOL_CALL 缺少 toolKey，nodeKey=" + node.getNodeKey());
            }
            String argsTemplate = cfg.get("argumentsTemplateJson") == null ? "{}" : String.valueOf(cfg.get("argumentsTemplateJson"));
            String args = renderTemplate(argsTemplate, root);

            ToolCallback tool = findToolByKey(toolKey)
                    .orElseThrow(() -> new NotFoundException("未找到工具回调，toolKey=" + toolKey));

            // 绑定 allowlist + run 信息（Workflow 场景）
            Set<String> allowedToolKeys = parseAllowedToolKeys(cfg.get("allowedToolKeysJson"));
            // 对 TOOL_CALL 节点：若未配置 allowlist，默认只放行自身
            if (allowedToolKeys == null) {
                allowedToolKeys = Set.of(toolKey);
            }
            GatewayToolBindingContextHolder.setWorkflow(null, sessionId, wf.getId(), version.getId(), node.getNodeKey(), allowedToolKeys);
            try {
                String result = tool.call(args);
                stepOutputs.put(node.getNodeKey() + ".toolKey", toolKey);
                stepOutputs.put(node.getNodeKey() + ".toolResult", result);
                return NodeResult.text(result, System.currentTimeMillis() - start);
            } finally {
                GatewayToolBindingContextHolder.clear();
            }
        }

        if ("LLM".equals(type) || "OUTPUT".equals(type)) {
            ModelConfig model = resolveModel(orgId, cfg);
            ChatClient chatClient = buildChatClient(model, cfg, workflowAdvisors);
            String system = cfg.get("systemPrompt") == null ? "" : String.valueOf(cfg.get("systemPrompt"));
            String userTemplate = cfg.get("userTemplate") == null ? "{{input}}" : String.valueOf(cfg.get("userTemplate"));
            String user = renderTemplate(userTemplate, root);

            List<SystemMessage> systemMessages = new ArrayList<>();
            if (StringUtils.hasText(system)) {
                systemMessages.add(new SystemMessage(system));
            }

            // RAG 注入：可显式引用某个 RAG 节点输出
            String ragFrom = cfg.get("ragFromNodeKey") == null ? null : String.valueOf(cfg.get("ragFromNodeKey"));
            if (StringUtils.hasText(ragFrom)) {
                Object ragTextObj = stepOutputs.get(ragFrom + ".ragText");
                if (ragTextObj != null) {
                    String ragText = String.valueOf(ragTextObj);
                    if (StringUtils.hasText(ragText)) {
                        systemMessages.add(new SystemMessage("你可以参考以下【参考文档】回答用户问题：\n" + ragText));
                    }
                }
            }

            boolean outputContract = "OUTPUT".equals(type) || "CONTRACT".equalsIgnoreCase(String.valueOf(cfg.get("outputMode")));
            if (outputContract) {
                systemMessages.add(new SystemMessage(outputSupport.contractInstruction()));
            }

            if (StringUtils.hasText(approvedToolResult)) {
                systemMessages.add(new SystemMessage("已执行并通过审批的工具调用结果如下（仅供继续推理，不再触发工具调用）:\n" + truncate(approvedToolResult, 8000)));
            }

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.addAll(systemMessages);
            messages.add(new UserMessage(user));
            Prompt prompt = new Prompt(messages);

            Set<String> allowedToolKeys = parseAllowedToolKeys(cfg.get("allowedToolKeysJson"));
            Long modelId = model == null ? null : model.getId();
            GatewayToolBindingContextHolder.setWorkflow(modelId, sessionId, wf.getId(), version.getId(), node.getNodeKey(), allowedToolKeys);
            try {
                ChatResponse resp = chatClient.prompt(prompt).call().chatResponse();
                String raw = extractText(resp);

                if (!outputContract) {
                    String safe = sanitizeAndLimit(raw).text;
                    stepOutputs.put(node.getNodeKey() + ".text", safe);
                    NodeResult r = NodeResult.text(safe, System.currentTimeMillis() - start);
                    r.modelIdUsed = model == null ? null : model.getId();
                    r.modelNameUsed = model == null ? null : model.getModelName();
                    applyUsage(r, resp);
                    return r;
                }

                PlatformContractV1 parsed = outputSupport.parseOrNull(raw);
                if (parsed == null) {
                    parsed = repairOnce(chatClient, raw);
                }
                if (parsed == null) {
                    throw new BusinessException("OUTPUT 节点输出无法解析为 PlatformContractV1");
                }
                stepOutputs.put(node.getNodeKey() + ".contract.answer", parsed.getAnswer());
                NodeResult r = NodeResult.contract(parsed, sanitizeAndLimit(raw).text, System.currentTimeMillis() - start);
                r.modelIdUsed = model == null ? null : model.getId();
                r.modelNameUsed = model == null ? null : model.getModelName();
                applyUsage(r, resp);
                return r;
            } finally {
                GatewayToolBindingContextHolder.clear();
            }
        }

        throw new BusinessException("不支持的节点类型: " + type);
    }

    private void applyUsage(NodeResult r, ChatResponse resp) {
        if (r == null || resp == null || resp.getMetadata() == null || resp.getMetadata().getUsage() == null) {
            return;
        }
        try {
            org.springframework.ai.chat.metadata.Usage u = resp.getMetadata().getUsage();
            if (u == null) {
                return;
            }
            r.promptTokens = u.getPromptTokens() == null ? 0 : u.getPromptTokens().intValue();
            r.completionTokens = u.getCompletionTokens() == null ? 0 : u.getCompletionTokens().intValue();
            r.totalTokens = u.getTotalTokens() == null ? 0 : u.getTotalTokens().intValue();
        } catch (Exception ignore) {
        }
    }

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
                                       org.springframework.ai.chat.client.advisor.api.CallAdvisor[] workflowAdvisors) {
        if (model == null) {
            throw new BusinessException("未找到可用模型");
        }
        Boolean toolEnabled = cfg.get("toolEnabled") == null ? null : Boolean.valueOf(String.valueOf(cfg.get("toolEnabled")));
        boolean enableTools = toolEnabled == null || toolEnabled;
        return chatClientAssemblyService.buildChatClient(model, enableTools, workflowAdvisors == null ? new org.springframework.ai.chat.client.advisor.api.CallAdvisor[0] : workflowAdvisors);
    }

    private ModelConfig resolveModel(Long orgId, Map<String, Object> cfg) {
        Object fixed = cfg.get("fixedModelId");
        if (fixed != null) {
            try {
                Long modelId = Long.valueOf(String.valueOf(fixed));
                ModelConfig model = modelConfigService.queryModelConfigById(new IdQuery(orgId, modelId));
                if (model != null && model.getOrgId() != null && !model.getOrgId().equals(orgId)) {
                    throw new BusinessException("模型不属于当前组织，modelId=" + modelId);
                }
                return model;
            } catch (NumberFormatException ignore) {
            }
        }
        List<ModelConfig> enabled = modelConfigService.queryEnabledModels(new EnabledQuery(orgId, true));
        if (enabled == null || enabled.isEmpty()) {
            throw new BusinessException("当前组织未配置可用模型");
        }
        return enabled.stream()
                .filter(m -> m != null && m.getOrgId() != null && m.getOrgId().equals(orgId))
                .max((a, b) -> Integer.compare(a.getPriority() == null ? 0 : a.getPriority(), b.getPriority() == null ? 0 : b.getPriority()))
                .orElseThrow(() -> new BusinessException("当前组织未配置可用模型"));
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
            if (cb instanceof com.xbk.knowledge.types.tool.ToolKeyAware aware) {
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
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
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
            List<String> list = objectMapper.readValue(s, new TypeReference<List<String>>() {});
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
            java.util.Set<String> set = new java.util.HashSet<>();
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
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (list == null) {
                return null;
            }
            return new java.util.HashSet<>(list);
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

    private void saveRunContextSnapshot(Long orgId,
                                        String runId,
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
                    .orgId(orgId)
                    .runId(runId)
                    .status("SAVED")
                    .snapshotJson(json)
                    .build());
        } catch (Exception e) {
            log.warn("保存 workflow_run_context 快照失败，runId: {}", runId, e);
        }
    }

    private WorkflowSnapshot loadSnapshot(Long orgId, String runId) {
        if (workflowRunContextRepository == null || orgId == null || !StringUtils.hasText(runId)) {
            return null;
        }
        return workflowRunContextRepository.findByRunId(orgId, runId)
                .map(ctx -> {
                    if (ctx == null || !StringUtils.hasText(ctx.getSnapshotJson())) {
                        return null;
                    }
                    try {
                        Map<String, Object> map = objectMapper.readValue(ctx.getSnapshotJson(), new TypeReference<Map<String, Object>>() {});
                        Long sessionId = map.get("sessionId") == null ? null : Long.valueOf(String.valueOf(map.get("sessionId")));
                        String content = map.get("content") == null ? "" : String.valueOf(map.get("content"));
                        Map<String, Object> vars = map.get("variables") instanceof Map<?, ?> m ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
                        Map<String, Object> steps = map.get("stepOutputs") instanceof Map<?, ?> m ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
                        String pendingNodeKey = map.get("pendingNodeKey") == null ? null : String.valueOf(map.get("pendingNodeKey"));
                        return new WorkflowSnapshot(sessionId, content, vars, steps, pendingNodeKey);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    private PlatformContractV1 buildContractWithSteps(Long orgId,
                                                      Workflow wf,
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
                        .promptTokens(nr.getPromptTokens())
                        .completionTokens(nr.getCompletionTokens())
                        .totalTokens(nr.getTotalTokens())
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
                .orgId(orgId)
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
