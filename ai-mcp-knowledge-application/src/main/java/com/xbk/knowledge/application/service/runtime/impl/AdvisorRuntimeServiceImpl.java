package com.xbk.knowledge.application.service.runtime.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.runtime.AdvisorRuntimeService;
import com.xbk.knowledge.config.ai.RequestResponseLoggingAdvisor;
import com.xbk.knowledge.config.ai.ToolCallLoggingAdvisor;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingQuery;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingView;
import com.xbk.knowledge.domain.advisor.adapter.repository.AdvisorBindingRepository;
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
 * Advisor 运行时装配实现。
 *
 * 说明：
 * - advisor_binding 负责“对哪个目标生效 + 顺序 + 是否启用”
 * - advisor 负责“类型 + 配置 + 是否启用”
 
  * @author xiexu
  */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisorRuntimeServiceImpl implements AdvisorRuntimeService {

    private final AdvisorBindingRepository advisorBindingRepository;
    private final ObjectMapper objectMapper;

    private final ChatMemory chatMemory;
    private final ChatMemoryRepository chatMemoryRepository;
    private final RequestResponseLoggingAdvisor requestResponseLoggingAdvisor;
    private final ToolCallLoggingAdvisor toolCallLoggingAdvisor;

    private final ConcurrentHashMap<BindingKey, List<AdvisorBindingView>> bindingCache = new ConcurrentHashMap<>();

    /**
     * resolveForAgentVersion。
     *
     * @param scopeId 参数
     * @param agentVersionId 参数
     * @param runId 参数
     * @param sessionId 参数
     * @return 返回结果
     */
    @Override
    public CallAdvisor[] resolveForAgentVersion(Long agentVersionId, String runId, Long sessionId) {
        return resolveForTarget("AGENT_VERSION", agentVersionId, runId, sessionId);
    }

    /**
     * resolveForWorkflowVersion。
     *
     * @param scopeId 参数
     * @param workflowVersionId 参数
     * @param runId 参数
     * @param sessionId 参数
     * @return 返回结果
     */
    @Override
    public CallAdvisor[] resolveForWorkflowVersion(Long workflowVersionId, String runId, Long sessionId) {
        return resolveForTarget("WORKFLOW_VERSION", workflowVersionId, runId, sessionId);
    }

    /**
     * evictBindingCache。
     *
     * @param scopeId 参数
     * @param bindType 参数
     * @param bindTargetId 参数
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
     * evictAll。
     *
     * @param scopeId 参数
     */
    @Override
    public void evictAll() {
        bindingCache.clear();
    }

    private CallAdvisor[] resolveForTarget(String bindType, Long bindTargetId, String runId, Long sessionId) {
        if (!StringUtils.hasText(bindType) || bindTargetId == null) {
            return new CallAdvisor[0];
        }
        List<AdvisorBindingView> views = loadBindingViews(bindType, bindTargetId);
        if (views.isEmpty()) {
            return new CallAdvisor[0];
        }

        List<CallAdvisor> advisors = new ArrayList<>();
        for (AdvisorBindingView v : views) {
            if (v == null) {
                continue;
            }
            if (!isEnabled(v)) {
                continue;
            }
            CallAdvisor advisor = instantiate(v, runId, sessionId);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }
        return advisors.toArray(new CallAdvisor[0]);
    }

    private List<AdvisorBindingView> loadBindingViews(String bindType, Long bindTargetId) {
        BindingKey key = new BindingKey(bindType.trim().toUpperCase(Locale.ROOT), bindTargetId);
        List<AdvisorBindingView> cached = bindingCache.get(key);
        if (cached != null) {
            return cached;
        }
        AdvisorBindingQuery q = new AdvisorBindingQuery(key.bindType, bindTargetId);
        List<AdvisorBindingView> views = advisorBindingRepository.listBindingViews(q);
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

    private boolean isEnabled(AdvisorBindingView v) {
        Integer bindingEnabled = v.getBindingEnabled();
        Integer advisorEnabled = v.getAdvisorEnabled();
        return (bindingEnabled == null || bindingEnabled == 1) && (advisorEnabled == null || advisorEnabled == 1);
    }

    private CallAdvisor instantiate(AdvisorBindingView v, String runId, Long sessionId) {
        String type = v.getAdvisorType();
        if (!StringUtils.hasText(type)) {
            return null;
        }
        type = type.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "CHAT_MEMORY" -> buildChatMemoryAdvisor(v, runId, sessionId);
            case "REQUEST_RESPONSE_LOG" -> requestResponseLoggingAdvisor;
            case "TOOL_CALL_LOG" -> toolCallLoggingAdvisor;
            default -> {
                log.warn("未知 advisorType: {} (advisorCode={})", type, v.getAdvisorCode());
                yield null;
            }
        };
    }

    private CallAdvisor buildChatMemoryAdvisor(AdvisorBindingView v, String runId, Long sessionId) {
        String conversationId = buildConversationId(v, runId, sessionId);
        int maxMessages = readIntConfig(v.getAdvisorConfigJson(), "maxMessages", -1);

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

    private String buildConversationId(AdvisorBindingView v, String runId, Long sessionId) {
        String prefix = readStringConfig(v.getAdvisorConfigJson(), "prefix", null);
        String strategy = readStringConfig(v.getAdvisorConfigJson(), "conversationIdFrom", null);
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
            throw new BusinessException("advisor.configJson 非法 JSON");
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
