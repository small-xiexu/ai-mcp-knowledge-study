package com.xbk.knowledge.application.service.runtime.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.runtime.AgentEnhancerRuntimeService;
import com.xbk.knowledge.config.ai.RequestResponseLoggingAgentEnhancer;
import com.xbk.knowledge.config.ai.ToolCallLoggingAgentEnhancer;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingQuery;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingView;
import com.xbk.knowledge.domain.agentenhancer.adapter.repository.AgentEnhancerBindingRepository;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AgentEnhancer 运行时装配实现。
 *
 * 说明：
 * - agent_enhancer_binding 负责“对哪个目标生效 + 顺序 + 是否启用”
 * - agent_enhancer 负责“类型 + 配置 + 是否启用”
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEnhancerRuntimeServiceImpl implements AgentEnhancerRuntimeService {

    private final AgentEnhancerBindingRepository agentEnhancerBindingRepository;
    private final ObjectMapper objectMapper;

    private final ChatMemory chatMemory;
    private final ChatMemoryRepository chatMemoryRepository;
    private final RequestResponseLoggingAgentEnhancer requestResponseLoggingAgentEnhancer;
    private final ToolCallLoggingAgentEnhancer toolCallLoggingAgentEnhancer;

    private final ConcurrentHashMap<BindingKey, List<AgentEnhancerBindingView>> bindingCache = new ConcurrentHashMap<>();

    /**
     * 解析 Agent 版本绑定的 AgentEnhancer 链路。
     *
     * @param agentVersionId Agent 版本 ID。
     * @param runId 运行 ID
     * @param sessionId 会话 ID
     * @return 返回 CallAdvisor[] 数据。
     */
    @Override
    public CallAdvisor[] resolveForAgentVersion(Long agentVersionId, String runId, Long sessionId) {
        return resolveForTarget("AGENT_VERSION", agentVersionId, runId, sessionId);
    }

    /**
     * 解析 Workflow 版本绑定的 AgentEnhancer 链路。
     *
     * @param workflowVersionId 工作流版本 ID。
     * @param runId 运行 ID
     * @param sessionId 会话 ID
     * @return 返回 CallAdvisor[] 数据。
     */
    @Override
    public CallAdvisor[] resolveForWorkflowVersion(Long workflowVersionId, String runId, Long sessionId) {
        return resolveForTarget("WORKFLOW_VERSION", workflowVersionId, runId, sessionId);
    }

    /**
     * 按绑定类型清理 AgentEnhancer 绑定缓存。
     *
     * @param bindType 绑定类型
     * @param bindTargetId 绑定目标 ID
     */
    @Override
    public void evictBindingCache(String bindType, Long bindTargetId) {
        if (!StringUtils.hasText(bindType) || bindTargetId == null) {
            return;
        }
        BindingKey key = new BindingKey(bindType.trim().toUpperCase(Locale.ROOT), bindTargetId);
        bindingCache.remove(key);
    }

    /**
     * 清空全部 AgentEnhancer 绑定缓存。
     *
     *
     */
    @Override
    public void evictAll() {
        bindingCache.clear();
    }

    private CallAdvisor[] resolveForTarget(String bindType, Long bindTargetId, String runId, Long sessionId) {
        if (!StringUtils.hasText(bindType) || bindTargetId == null) {
            return new CallAdvisor[0];
        }
        List<AgentEnhancerBindingView> views = loadBindingViews(bindType, bindTargetId);
        if (views.isEmpty()) {
            return new CallAdvisor[0];
        }

        List<CallAdvisor> agentEnhancers = new ArrayList<>();
        for (AgentEnhancerBindingView v : views) {
            if (v == null) {
                continue;
            }
            if (!isEnabled(v)) {
                continue;
            }
            CallAdvisor agentEnhancer = instantiate(v, runId, sessionId);
            if (agentEnhancer != null) {
                agentEnhancers.add(agentEnhancer);
            }
        }
        return agentEnhancers.toArray(new CallAdvisor[0]);
    }

    private List<AgentEnhancerBindingView> loadBindingViews(String bindType, Long bindTargetId) {
        BindingKey key = new BindingKey(bindType.trim().toUpperCase(Locale.ROOT), bindTargetId);
        List<AgentEnhancerBindingView> cached = bindingCache.get(key);
        if (cached != null) {
            return cached;
        }
        AgentEnhancerBindingQuery q = new AgentEnhancerBindingQuery(key.bindType, bindTargetId);
        List<AgentEnhancerBindingView> views = agentEnhancerBindingRepository.listBindingViews(q);
        if (views == null || views.isEmpty()) {
            bindingCache.put(key, Collections.emptyList());
            return Collections.emptyList();
        }
        // defensive sort（DB 已 order by）
        views = views.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(v -> v.getOrderNo() == null ? 0 : v.getOrderNo()))
                .toList();
        bindingCache.put(key, views);
        return views;
    }

    private boolean isEnabled(AgentEnhancerBindingView v) {
        Integer bindingEnabled = v.getBindingEnabled();
        Integer agentEnhancerEnabled = v.getAgentEnhancerEnabled();
        return (bindingEnabled == null || bindingEnabled == 1) && (agentEnhancerEnabled == null || agentEnhancerEnabled == 1);
    }

    private CallAdvisor instantiate(AgentEnhancerBindingView v, String runId, Long sessionId) {
        String type = v.getAgentEnhancerType();
        if (!StringUtils.hasText(type)) {
            return null;
        }
        type = type.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "CHAT_MEMORY" -> buildChatMemoryAdvisor(v, runId, sessionId);
            case "REQUEST_RESPONSE_LOG" -> requestResponseLoggingAgentEnhancer;
            case "TOOL_CALL_LOG" -> toolCallLoggingAgentEnhancer;
            default -> {
                log.warn("未知 agentEnhancerType: {} (agentEnhancerCode={})", type, v.getAgentEnhancerCode());
                yield null;
            }
        };
    }

    private CallAdvisor buildChatMemoryAdvisor(AgentEnhancerBindingView v, String runId, Long sessionId) {
        String conversationId = buildConversationId(v, runId, sessionId);
        int maxMessages = readIntConfig(v.getAgentEnhancerConfigJson(), "maxMessages", -1);

        ChatMemory memoryToUse = this.chatMemory;
        if (maxMessages > 0) {
            memoryToUse = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(chatMemoryRepository)
                    .maxMessages(maxMessages)
                    .build();
        }
        return PromptChatMemoryAdvisor.builder(memoryToUse)
                .conversationId(conversationId)
                .build();
    }

    private String buildConversationId(AgentEnhancerBindingView v, String runId, Long sessionId) {
        String prefix = readStringConfig(v.getAgentEnhancerConfigJson(), "prefix", null);
        String strategy = readStringConfig(v.getAgentEnhancerConfigJson(), "conversationIdFrom", null);
        String base;
        if ("RUN_ID".equalsIgnoreCase(strategy)) {
            base = StringUtils.hasText(runId) ? runId : String.valueOf(sessionId == null ? 0 : sessionId);
        } else {
            // 默认优先 sessionId，兜底 runId
            if (sessionId != null) {
                base = String.valueOf(sessionId);
            } else if (StringUtils.hasText(runId)) {
                base = runId;
            } else {
                base = "0";
            }
        }
        if (!StringUtils.hasText(prefix)) {
            return base;
        }
        return prefix + base;
    }

    private int readIntConfig(String json, String field, int defaultVal) {
        if (!StringUtils.hasText(json) || !StringUtils.hasText(field)) {
            return defaultVal;
        }
        try {
            JsonNode n = objectMapper.readTree(json);
            JsonNode v = n == null ? null : n.get(field);
            if (v == null || v.isNull()) {
                return defaultVal;
            }
            return v.asInt(defaultVal);
        } catch (Exception e) {
            throw new BusinessException("agentEnhancer.configJson 非法 JSON");
        }
    }

    private String readStringConfig(String json, String field, String defaultVal) {
        if (!StringUtils.hasText(json) || !StringUtils.hasText(field)) {
            return defaultVal;
        }
        try {
            JsonNode n = objectMapper.readTree(json);
            JsonNode v = n == null ? null : n.get(field);
            if (v == null || v.isNull()) {
                return defaultVal;
            }
            String s = v.asText();
            return StringUtils.hasText(s) ? s : defaultVal;
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private record BindingKey(String bindType, Long bindTargetId) {
    }
}
