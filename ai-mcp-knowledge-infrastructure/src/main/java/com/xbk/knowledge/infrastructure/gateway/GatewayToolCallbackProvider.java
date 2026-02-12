package com.xbk.knowledge.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolBinding;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.gateway.ToolBindingQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolBindingRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolRegistryRepository;
import com.xbk.knowledge.domain.service.gateway.GatewayToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gateway ToolCallback 提供者
 *
 * 职责：将 Gateway 配置的 HTTP 工具转换为 Spring AI 的 ToolCallback，
 * 注入到 AI 模型调用链路中。支持工具绑定过滤（按模型/会话维度控制工具可见性）
 *
 * 为什么需要：Gateway 工具以 HTTP 配置形式存储在数据库中，需要适配为 Spring AI
 * 的 FunctionToolCallback 才能被 ChatClient 识别和调用
 *
 * @author xiexu
 */
@Slf4j
@Component("gatewayToolCallbackProvider")
@RequiredArgsConstructor
public class GatewayToolCallbackProvider implements ToolCallbackProvider {

    private static final String CALL_ID_MDC_KEY = "gatewayToolCallId";
    private static final String BIND_TYPE_MODEL = "MODEL";
    private static final String BIND_TYPE_SESSION = "SESSION";

    private final McpGatewayRepository gatewayRepository;
    private final McpToolRegistryRepository toolRegistryRepository;
    private final McpToolBindingRepository toolBindingRepository;
    private final GatewayToolService gatewayToolService;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有可用的 Gateway 工具回调
     * 流程：加载已启用网关 → 获取工具定义 → 应用绑定过滤 → 去重 → 构建 FunctionToolCallback
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        List<McpGateway> enabledGateways = gatewayRepository.findAllEnabled();
        if (enabledGateways == null || enabledGateways.isEmpty()) {
            return new ToolCallback[0];
        }

        List<ToolCandidate> allCandidates = new ArrayList<>();
        Map<String, Map<String, GatewayToolService.ToolDefinition>> toolDefinitionByGateway = loadToolDefinitions(enabledGateways);

        for (McpGateway gateway : enabledGateways) {
            if (gateway == null || !StringUtils.hasText(gateway.getGatewayId())) {
                continue;
            }
            List<McpToolRegistry> toolRegistries = toolRegistryRepository.findEnabledByGatewayId(
                    new com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery(gateway.getGatewayId())
            );
            if (toolRegistries == null || toolRegistries.isEmpty()) {
                continue;
            }
            Map<String, GatewayToolService.ToolDefinition> toolDefinitionMap = toolDefinitionByGateway.getOrDefault(
                    gateway.getGatewayId(),
                    Collections.emptyMap()
            );
            for (McpToolRegistry registry : toolRegistries) {
                if (registry == null || registry.getId() == null || !StringUtils.hasText(registry.getToolName())) {
                    continue;
                }
                GatewayToolService.ToolDefinition definition = toolDefinitionMap.get(registry.getToolName());
                allCandidates.add(buildToolCandidate(gateway.getGatewayId(), registry, definition));
            }
        }

        if (allCandidates.isEmpty()) {
            return new ToolCallback[0];
        }

        List<ToolCandidate> visibleCandidates = applyBindingFilter(allCandidates);
        if (visibleCandidates.isEmpty()) {
            return new ToolCallback[0];
        }

        List<ToolCallback> callbacks = new ArrayList<>();
        Set<String> dedupNames = new HashSet<>();
        for (ToolCandidate candidate : visibleCandidates) {
            if (!dedupNames.add(candidate.toolName)) {
                log.warn("发现重名 gateway 工具，已跳过后续重复项: {}", candidate.toolName);
                continue;
            }
            callbacks.add(buildToolCallback(candidate));
        }

        log.info("GatewayToolCallbackProvider 返回工具数量: {}", callbacks.size());
        return callbacks.toArray(new ToolCallback[0]);
    }

    /** 批量加载所有网关的工具定义，返回 gatewayId → (toolName → ToolDefinition) 映射 */
    private Map<String, Map<String, GatewayToolService.ToolDefinition>> loadToolDefinitions(List<McpGateway> gateways) {
        Map<String, Map<String, GatewayToolService.ToolDefinition>> result = new HashMap<>();
        for (McpGateway gateway : gateways) {
            if (gateway == null || !StringUtils.hasText(gateway.getGatewayId())) {
                continue;
            }
            try {
                List<GatewayToolService.ToolDefinition> definitions = gatewayToolService.listTools(gateway.getGatewayId());
                Map<String, GatewayToolService.ToolDefinition> map = new HashMap<>();
                for (GatewayToolService.ToolDefinition definition : definitions) {
                    if (definition == null || !StringUtils.hasText(definition.name())) {
                        continue;
                    }
                    map.put(definition.name(), definition);
                }
                result.put(gateway.getGatewayId(), map);
            } catch (Exception e) {
                log.warn("加载 gateway 工具定义失败，gatewayId: {}", gateway.getGatewayId(), e);
            }
        }
        return result;
    }

    /** 根据 ThreadLocal 中的绑定上下文过滤工具候选列表（无上下文时不过滤） */
    private List<ToolCandidate> applyBindingFilter(List<ToolCandidate> candidates) {
        GatewayToolBindingContextHolder.BindingContext context = GatewayToolBindingContextHolder.get();
        if (context == null) {
            return candidates;
        }

        Set<Long> boundToolIds = new HashSet<>();
        boolean hasBinding = false;

        Long modelId = context.getModelId();
        if (modelId != null) {
            List<McpToolBinding> modelBindings = toolBindingRepository.findByBindTypeAndTargetId(
                    new ToolBindingQuery(BIND_TYPE_MODEL, modelId)
            );
            if (modelBindings != null && !modelBindings.isEmpty()) {
                hasBinding = true;
                appendEnabledToolIds(boundToolIds, modelBindings);
            }
        }

        Long sessionId = context.getSessionId();
        if (sessionId != null) {
            List<McpToolBinding> sessionBindings = toolBindingRepository.findByBindTypeAndTargetId(
                    new ToolBindingQuery(BIND_TYPE_SESSION, sessionId)
            );
            if (sessionBindings != null && !sessionBindings.isEmpty()) {
                hasBinding = true;
                appendEnabledToolIds(boundToolIds, sessionBindings);
            }
        }

        if (!hasBinding) {
            return candidates;
        }

        List<ToolCandidate> filtered = new ArrayList<>();
        for (ToolCandidate candidate : candidates) {
            if (boundToolIds.contains(candidate.toolId)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    /** 从绑定列表中收集已启用的工具 ID */
    private void appendEnabledToolIds(Set<Long> collector, List<McpToolBinding> bindings) {
        for (McpToolBinding binding : bindings) {
            if (binding == null || binding.getToolId() == null) {
                continue;
            }
            if (Boolean.FALSE.equals(binding.getEnabled())) {
                continue;
            }
            collector.add(binding.getToolId());
        }
    }

    /** 将工具注册信息和定义组装为候选对象 */
    private ToolCandidate buildToolCandidate(String gatewayId,
                                             McpToolRegistry registry,
                                             GatewayToolService.ToolDefinition definition) {
        String inputSchema = "";
        if (definition != null && definition.inputSchema() != null && !definition.inputSchema().isEmpty()) {
            try {
                inputSchema = objectMapper.writeValueAsString(definition.inputSchema());
            } catch (Exception e) {
                log.warn("Gateway 工具 inputSchema 序列化失败，toolName: {}", registry.getToolName(), e);
            }
        }

        String description = StringUtils.hasText(registry.getToolDescription())
                ? registry.getToolDescription()
                : "";

        return new ToolCandidate(registry.getId(), gatewayId, registry.getToolName(), description, inputSchema);
    }

    /** 将候选对象构建为 FunctionToolCallback，内部封装工具调用逻辑和链路追踪 */
    private ToolCallback buildToolCallback(ToolCandidate candidate) {
        String inputSchema = candidate.inputSchema;
        if (!StringUtils.hasText(inputSchema)) {
            inputSchema = "{\"type\":\"object\",\"properties\":{}}";
        }

        ToolMetadata metadata = ToolMetadata.builder()
                .returnDirect(false)
                .build();

        return FunctionToolCallback
                .<Map<String, Object>, String>builder(candidate.toolName, arguments -> {
                    Map<String, Object> safeArgs = arguments == null ? Collections.emptyMap() : arguments;
                    String callId = UUID.randomUUID().toString().replace("-", "");
                    long startAt = System.nanoTime();
                    String previousCallId = MDC.get(CALL_ID_MDC_KEY);
                    MDC.put(CALL_ID_MDC_KEY, callId);
                    log.info("gateway_tool_call source=AI stage=start callId={} gatewayId={} toolName={} argsKeys={}",
                            callId,
                            candidate.gatewayId,
                            candidate.toolName,
                            safeArgs.keySet());

                    try {
                        GatewayToolService.ToolCallResult callResult = gatewayToolService.callTool(
                                candidate.gatewayId,
                                candidate.toolName,
                                safeArgs
                        );
                        long latencyMs = (System.nanoTime() - startAt) / 1_000_000;
                        log.info("gateway_tool_call source=AI stage=end callId={} gatewayId={} toolName={} argsKeys={} success={} errorCode={} latencyMs={}",
                                callId,
                                candidate.gatewayId,
                                candidate.toolName,
                                safeArgs.keySet(),
                                callResult.success(),
                                callResult.errorCode(),
                                latencyMs);

                        if (callResult.success()) {
                            return callResult.content();
                        }
                        String errorCode = callResult.errorCode() == null ? "TOOL_ERROR" : callResult.errorCode();
                        return "[" + errorCode.toUpperCase(Locale.ROOT) + "] " + callResult.content();
                    } catch (Exception e) {
                        long latencyMs = (System.nanoTime() - startAt) / 1_000_000;
                        log.error("gateway_tool_call source=AI stage=exception callId={} gatewayId={} toolName={} argsKeys={} success=false errorCode={} latencyMs={} error={}",
                                callId,
                                candidate.gatewayId,
                                candidate.toolName,
                                safeArgs.keySet(),
                                "TOOL_EXEC_FAILED",
                                latencyMs,
                                e.getMessage(),
                                e);
                        throw e;
                    } finally {
                        if (StringUtils.hasText(previousCallId)) {
                            MDC.put(CALL_ID_MDC_KEY, previousCallId);
                        } else {
                            MDC.remove(CALL_ID_MDC_KEY);
                        }
                    }
                })
                .description(candidate.description)
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema(inputSchema)
                .toolMetadata(metadata)
                .build();
    }

    /** 工具候选对象，承载构建 ToolCallback 所需的中间数据 */
    private static class ToolCandidate {

        private final Long toolId;
        private final String gatewayId;
        private final String toolName;
        private final String description;
        private final String inputSchema;

        private ToolCandidate(Long toolId,
                              String gatewayId,
                              String toolName,
                              String description,
                              String inputSchema) {
            this.toolId = toolId;
            this.gatewayId = gatewayId;
            this.toolName = toolName;
            this.description = description;
            this.inputSchema = inputSchema;
        }
    }
}
