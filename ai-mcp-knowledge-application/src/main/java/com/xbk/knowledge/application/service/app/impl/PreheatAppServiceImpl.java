package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.model.preheat.PreheatResult;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.application.service.app.McpServerConfigAppService;
import com.xbk.knowledge.application.service.app.PreheatAppService;
import com.xbk.knowledge.application.service.armory.factory.DefaultAiClientArmoryStrategyFactory;
import com.xbk.knowledge.application.service.runtime.AgentEnhancerRuntimeService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentClientProfileStep;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowGraphQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentVersionRepository;
import com.xbk.knowledge.domain.client.adapter.repository.ClientProfileRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowGraphRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowVersionRepository;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.tool.ToolKeyAware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 预热应用服务实现。
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreheatAppServiceImpl implements PreheatAppService {

    private final McpServerConfigAppService mcpServerConfigAppService;
    private final ToolCallbackProvider toolCallbackProvider;
    private final AgentEnhancerRuntimeService agentEnhancerRuntimeService;
    private final ObjectMapper objectMapper;
    private final ModelConfigAppService modelConfigAppService;
    private final DefaultAiClientArmoryStrategyFactory armoryStrategyFactory;

    private final AgentVersionRepository agentVersionRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowGraphRepository workflowGraphRepository;
    private final ClientProfileRepository clientProfileRepository;

    /**
     * 预热 Agent 版本运行资源。
     *
     * @param agentVersionId Agent 版本 ID。
     * @param refreshMcp 是否刷新 MCP 客户端。
     * @return 返回 PreheatResult 数据。
     */
    @Override
    public PreheatResult preheatAgentVersion(Long agentVersionId, boolean refreshMcp) {
        if (agentVersionId == null) {
            throw new IllegalArgumentException("agentVersionId 不能为空");
        }
        List<String> warnings = new ArrayList<>();

        AgentVersion v = agentVersionRepository.findById(new AgentVersionIdQuery(agentVersionId))
                .orElseThrow(() -> new NotFoundException("AgentVersion 不存在，id=" + agentVersionId));

        boolean mcpRefreshed = false;
        if (refreshMcp) {
            mcpServerConfigAppService.refreshEnabledServers();
            mcpRefreshed = true;
        }

        boolean toolsWarmed = warmToolCallbacks(warnings);
        warmChatClient(resolveAgentVersionModelIds(v), warnings);

        boolean agentEnhancersWarmed;
        try {
            agentEnhancerRuntimeService.resolveForAgentVersion(v.getId(), "preheat-agent-" + System.nanoTime(), null);
            agentEnhancersWarmed = true;
        } catch (Exception e) {
            agentEnhancersWarmed = false;
            warnings.add("AgentEnhancer 装配失败: " + safeMsg(e));
        }

        if (v.getWorkflowVersionId() != null) {
            warnings.add("该 AgentVersion 绑定了 WorkflowVersion，实际运行以 WorkflowVersion 绑定 AgentEnhancers 为准");
        }

        return PreheatResult.builder()
                .targetType("AGENT_VERSION")
                .targetId(agentVersionId)
                .mcpRefreshed(mcpRefreshed)
                .toolCallbacksWarmed(toolsWarmed)
                .agentEnhancersWarmed(agentEnhancersWarmed)
                .workflowValidated(false)
                .warnings(warnings)
                .build();
    }

    /**
     * 预热 Workflow 版本运行资源。
     *
     * @param workflowVersionId 工作流版本 ID。
     * @param refreshMcp 是否刷新 MCP 客户端。
     * @return 返回 PreheatResult 数据。
     */
    @Override
    public PreheatResult preheatWorkflowVersion(Long workflowVersionId, boolean refreshMcp) {
        if (workflowVersionId == null) {
            throw new IllegalArgumentException("workflowVersionId 不能为空");
        }
        List<String> warnings = new ArrayList<>();

        WorkflowVersion v = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));

        boolean mcpRefreshed = false;
        if (refreshMcp) {
            mcpServerConfigAppService.refreshEnabledServers();
            mcpRefreshed = true;
        }

        boolean toolsWarmed = warmToolCallbacks(warnings);
        warmChatClient(warnings);

        boolean agentEnhancersWarmed;
        try {
            agentEnhancerRuntimeService.resolveForWorkflowVersion(v.getId(), "preheat-wf-" + System.nanoTime(), null);
            agentEnhancersWarmed = true;
        } catch (Exception e) {
            agentEnhancersWarmed = false;
            warnings.add("AgentEnhancer 装配失败: " + safeMsg(e));
        }

        boolean validated = validateWorkflow(workflowVersionId, toolsWarmed, warnings);

        return PreheatResult.builder()
                .targetType("WORKFLOW_VERSION")
                .targetId(workflowVersionId)
                .mcpRefreshed(mcpRefreshed)
                .toolCallbacksWarmed(toolsWarmed)
                .agentEnhancersWarmed(agentEnhancersWarmed)
                .workflowValidated(validated)
                .warnings(warnings)
                .build();
    }

    private boolean warmToolCallbacks(List<String> warnings) {
        try {
            ToolCallback[] callbacks = toolCallbackProvider == null ? null : toolCallbackProvider.getToolCallbacks();
            int count = callbacks == null ? 0 : callbacks.length;
            if (count == 0) {
                warnings.add("未发现可用工具回调（可能尚未 refresh MCP 或当前无启用 server）");
            }
            return true;
        } catch (Exception e) {
            warnings.add("工具回调预热失败: " + safeMsg(e));
            return false;
        }
    }

    private boolean warmChatClient(List<String> warnings) {
        return warmChatClient(List.of(), warnings);
    }

    private boolean warmChatClient(List<Long> targetModelIds, List<String> warnings) {
        try {
            List<ModelConfig> models = modelConfigAppService.queryEnabledModels(new EnabledQuery(true));
            if (models == null || models.isEmpty()) {
                warnings.add("未发现可用模型，跳过 ChatClient 预热");
                return false;
            }
            List<ModelConfig> candidates = new ArrayList<>();
            if (targetModelIds != null && !targetModelIds.isEmpty()) {
                Set<Long> target = new LinkedHashSet<>(targetModelIds);
                for (ModelConfig model : models) {
                    if (model != null && model.getId() != null && target.contains(model.getId())) {
                        candidates.add(model);
                    }
                }
            }
            if (candidates.isEmpty()) {
                ModelConfig fallback = models.stream()
                        .filter(m -> m != null && m.getId() != null)
                        .sorted(Comparator.comparingLong(ModelConfig::getId))
                        .findFirst()
                        .orElse(null);
                if (fallback != null) {
                    candidates.add(fallback);
                }
            }
            if (candidates.isEmpty()) {
                warnings.add("可用模型列表为空，跳过 ChatClient 预热");
                return false;
            }
            for (ModelConfig selected : candidates) {
                boolean enableTools = selected.getToolEnabled() == null || Boolean.TRUE.equals(selected.getToolEnabled());
                armoryStrategyFactory.preheat(selected, enableTools);
            }
            return true;
        } catch (Exception e) {
            warnings.add("ChatClient 预热失败: " + safeMsg(e));
            return false;
        }
    }

    /**
     * 解析Agent版本模型ID列表。
     *
     * @param version 工作流版本。
     * @return 返回解析后的模型配置。
     */
    private List<Long> resolveAgentVersionModelIds(AgentVersion version) {
        if (version == null) {
            return List.of();
        }
        Set<Long> modelIds = new LinkedHashSet<>();
        if (version.getClientProfileId() != null) {
            List<ClientProfileStep> steps = clientProfileRepository.listSteps(version.getClientProfileId());
            if (steps != null) {
                for (ClientProfileStep step : steps) {
                    if (step != null && step.getModelId() != null) {
                        modelIds.add(step.getModelId());
                    }
                }
            }
        }
        if (StringUtils.hasText(version.getClientChainJson())) {
            try {
                List<AgentClientProfileStep> steps = objectMapper.readValue(version.getClientChainJson(), new TypeReference<List<AgentClientProfileStep>>() {});
                if (steps != null) {
                    for (AgentClientProfileStep step : steps) {
                        if (step != null && step.getModelId() != null) {
                            modelIds.add(step.getModelId());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析 clientChainJson 失败，preheat 将忽略链路模型提取，agentVersionId={}", version.getId(), e);
            }
        }
        return new ArrayList<>(modelIds);
    }

    private boolean validateWorkflow(Long workflowVersionId, boolean toolsReady, List<String> warnings) {
        try {
            List<WorkflowNode> nodes = workflowGraphRepository.listNodes(WorkflowGraphQuery.builder()
                    .workflowVersionId(workflowVersionId)
                    .build());
            if (nodes == null || nodes.isEmpty()) {
                warnings.add("Workflow 图为空（缺少 nodes）");
                return false;
            }

            boolean hasStart = false;
            Set<String> toolKeys = new HashSet<>();
            for (WorkflowNode n : nodes) {
                if (n == null) {
                    continue;
                }
                String type = n.getNodeType() == null ? "" : n.getNodeType().trim().toUpperCase(Locale.ROOT);
                if ("START".equals(type)) {
                    hasStart = true;
                }

                // config_json 语法校验
                String cfgJson = n.getConfigJson();
                if (StringUtils.hasText(cfgJson)) {
                    try {
                        objectMapper.readTree(cfgJson);
                    } catch (Exception e) {
                        warnings.add("节点 configJson 非法 JSON，nodeKey=" + n.getNodeKey());
                    }
                }

                if ("TOOL_CALL".equals(type)) {
                    String toolKey = extractToolKey(cfgJson);
                    if (!StringUtils.hasText(toolKey)) {
                        warnings.add("TOOL_CALL 节点缺少 toolKey，nodeKey=" + n.getNodeKey());
                    } else {
                        toolKeys.add(toolKey);
                    }
                }
            }
            if (!hasStart) {
                warnings.add("Workflow 图缺少 START 节点");
                return false;
            }

            if (toolsReady && !toolKeys.isEmpty()) {
                Set<String> existed = listAvailableToolKeys();
                for (String k : toolKeys) {
                    if (!existed.contains(k)) {
                        warnings.add("未找到目标工具回调，toolKey=" + k);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            throw new BusinessException("Workflow 校验失败: " + safeMsg(e));
        }
    }

    /**
     * 提取工具 Key。
     *
     * @param cfgJson 配置JSON。
     * @return 返回标识Key。
     */
    private String extractToolKey(String cfgJson) {
        if (!StringUtils.hasText(cfgJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(cfgJson).path("toolKey").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> listAvailableToolKeys() {
        ToolCallback[] callbacks = toolCallbackProvider == null ? null : toolCallbackProvider.getToolCallbacks();
        if (callbacks == null || callbacks.length == 0) {
            return Set.of();
        }
        Set<String> keys = new HashSet<>();
        for (ToolCallback cb : callbacks) {
            if (cb instanceof ToolKeyAware aware) {
                if (StringUtils.hasText(aware.toolKey())) {
                    keys.add(aware.toolKey());
                }
            }
        }
        return keys;
    }

    private String safeMsg(Exception e) {
        return e == null ? "" : (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
}
