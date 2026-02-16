package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.model.preheat.PreheatResult;
import com.xbk.knowledge.application.service.app.McpServerConfigAppService;
import com.xbk.knowledge.application.service.app.PreheatAppService;
import com.xbk.knowledge.application.service.runtime.AdvisorRuntimeService;
import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNode;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowGraphQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.repository.agent.AgentVersionRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowGraphRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowVersionRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 预热应用服务实现。
 
  * @author xiexu
  */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreheatAppServiceImpl implements PreheatAppService {

    private final McpServerConfigAppService mcpServerConfigAppService;
    private final ToolCallbackProvider toolCallbackProvider;
    private final AdvisorRuntimeService advisorRuntimeService;
    private final ObjectMapper objectMapper;

    private final AgentVersionRepository agentVersionRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowGraphRepository workflowGraphRepository;

    /**
     * preheatAgentVersion。
     *
     * @param orgId 参数
     * @param agentVersionId 参数
     * @param refreshMcp 参数
     * @return 返回结果
     */
    @Override
    public PreheatResult preheatAgentVersion(Long orgId, Long agentVersionId, boolean refreshMcp) {
        if (orgId == null || agentVersionId == null) {
            throw new IllegalArgumentException("orgId/agentVersionId 不能为空");
        }
        List<String> warnings = new ArrayList<>();

        AgentVersion v = agentVersionRepository.findById(new AgentVersionIdQuery(orgId, agentVersionId))
                .orElseThrow(() -> new NotFoundException("AgentVersion 不存在，id=" + agentVersionId));

        boolean mcpRefreshed = false;
        if (refreshMcp) {
            mcpServerConfigAppService.refreshEnabledServers();
            mcpRefreshed = true;
        }

        boolean toolsWarmed = warmToolCallbacks(warnings);

        boolean advisorsWarmed;
        try {
            advisorRuntimeService.resolveForAgentVersion(orgId, v.getId(), "preheat-agent-" + System.nanoTime(), null);
            advisorsWarmed = true;
        } catch (Exception e) {
            advisorsWarmed = false;
            warnings.add("Advisor 装配失败: " + safeMsg(e));
        }

        if (v.getWorkflowVersionId() != null) {
            warnings.add("该 AgentVersion 绑定了 WorkflowVersion，实际运行以 WorkflowVersion 绑定 Advisors 为准");
        }

        return PreheatResult.builder()
                .orgId(orgId)
                .targetType("AGENT_VERSION")
                .targetId(agentVersionId)
                .mcpRefreshed(mcpRefreshed)
                .toolCallbacksWarmed(toolsWarmed)
                .advisorsWarmed(advisorsWarmed)
                .workflowValidated(false)
                .warnings(warnings)
                .build();
    }

    /**
     * preheatWorkflowVersion。
     *
     * @param orgId 参数
     * @param workflowVersionId 参数
     * @param refreshMcp 参数
     * @return 返回结果
     */
    @Override
    public PreheatResult preheatWorkflowVersion(Long orgId, Long workflowVersionId, boolean refreshMcp) {
        if (orgId == null || workflowVersionId == null) {
            throw new IllegalArgumentException("orgId/workflowVersionId 不能为空");
        }
        List<String> warnings = new ArrayList<>();

        WorkflowVersion v = workflowVersionRepository.findById(WorkflowVersionIdQuery.builder().orgId(orgId).id(workflowVersionId).build())
                .orElseThrow(() -> new NotFoundException("WorkflowVersion 不存在，id=" + workflowVersionId));

        boolean mcpRefreshed = false;
        if (refreshMcp) {
            mcpServerConfigAppService.refreshEnabledServers();
            mcpRefreshed = true;
        }

        boolean toolsWarmed = warmToolCallbacks(warnings);

        boolean advisorsWarmed;
        try {
            advisorRuntimeService.resolveForWorkflowVersion(orgId, v.getId(), "preheat-wf-" + System.nanoTime(), null);
            advisorsWarmed = true;
        } catch (Exception e) {
            advisorsWarmed = false;
            warnings.add("Advisor 装配失败: " + safeMsg(e));
        }

        boolean validated = validateWorkflow(orgId, workflowVersionId, toolsWarmed, warnings);

        return PreheatResult.builder()
                .orgId(orgId)
                .targetType("WORKFLOW_VERSION")
                .targetId(workflowVersionId)
                .mcpRefreshed(mcpRefreshed)
                .toolCallbacksWarmed(toolsWarmed)
                .advisorsWarmed(advisorsWarmed)
                .workflowValidated(validated)
                .warnings(warnings)
                .build();
    }

    private boolean warmToolCallbacks(List<String> warnings) {
        try {
            ToolCallback[] callbacks = toolCallbackProvider == null ? null : toolCallbackProvider.getToolCallbacks();
            int count = callbacks == null ? 0 : callbacks.length;
            if (count == 0) {
                warnings.add("未发现可用工具回调（可能尚未 refresh MCP 或当前 org 无启用 server）");
            }
            return true;
        } catch (Exception e) {
            warnings.add("工具回调预热失败: " + safeMsg(e));
            return false;
        }
    }

    private boolean validateWorkflow(Long orgId, Long workflowVersionId, boolean toolsReady, List<String> warnings) {
        try {
            List<WorkflowNode> nodes = workflowGraphRepository.listNodes(WorkflowGraphQuery.builder()
                    .orgId(orgId)
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

