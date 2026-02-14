package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.agent.AgentRuntimeChatRequest;
import com.xbk.knowledge.api.dto.agent.AgentRuntimeInvokeRequest;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.contract.PlatformStreamEvent;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Agent 运行入口 Controller（按 agentCode 路由）。
 *
 * 职责：对外提供按 agentCode 调用的运行入口（同步/流式），并统一注入 org 上下文。
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/agents/{agentCode}")
@RequiredArgsConstructor
public class AgentRuntimeController {

    private final AgentRuntimeAppService agentRuntimeAppService;

    /**
     * 同步对话调用（返回 Platform Contract v1）。
     *
     * @param agentCode Agent 对外编码
     * @param request   对话请求
     * @return 平台标准结构化结果
     */
    @PostMapping("/chat")
    @SaCheckPermission("agent:invoke")
    public Result<PlatformContractV1> chat(@PathVariable("agentCode") String agentCode,
                                          @Valid @RequestBody AgentRuntimeChatRequest request) {
        Long orgId = currentOrgId();
        PlatformContractV1 result = agentRuntimeAppService.chat(
                orgId,
                agentCode,
                request.getSessionId(),
                request.getContent(),
                request.getRagTagsJson()
        );
        return Result.success(result);
    }

    /**
     * 流式对话调用（SSE：delta + final）。
     *
     * @param agentCode     Agent 对外编码
     * @param request       对话请求
     * @param httpResponse  HTTP 响应（用于设置 SSE 相关 Header）
     * @return SSE emitter
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckPermission("agent:invoke")
    public SseEmitter stream(@PathVariable("agentCode") String agentCode,
                             @Valid @RequestBody AgentRuntimeChatRequest request,
                             HttpServletResponse httpResponse) {
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setHeader("Connection", "keep-alive");
        httpResponse.setHeader("X-Accel-Buffering", "no");

        Long orgId = currentOrgId();
        SseEmitter emitter = new SseEmitter(0L);

        agentRuntimeAppService.stream(
                orgId,
                agentCode,
                request.getSessionId(),
                request.getContent(),
                request.getRagTagsJson()
        ).subscribe(
                event -> {
                    try {
                        if (event == null) {
                            return;
                        }
                        String name = event.getName();
                        Object data = event.getData();
                        if (data == null) {
                            return;
                        }
                        if (data instanceof String text && text.isEmpty() && "delta".equals(name)) {
                            return;
                        }
                        emitter.send(SseEmitter.event().name(name).data(data));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );
        return emitter;
    }

    /**
     * 通用 invoke（内部触发或外部调用统一入口）。
     *
     * 说明：当 request.orgId 为空时使用当前上下文 orgId；否则按 request.orgId 运行（需配合 org 治理规则）。
     *
     * @param agentCode Agent 对外编码
     * @param request   调用请求
     * @return 平台标准结构化结果
     */
    @PostMapping("/invoke")
    @SaCheckPermission("agent:invoke")
    public Result<PlatformContractV1> invoke(@PathVariable("agentCode") String agentCode,
                                            @Valid @RequestBody AgentRuntimeInvokeRequest request) {
        Long orgId = request.getOrgId() != null ? request.getOrgId() : currentOrgId();
        PlatformContractV1 result = agentRuntimeAppService.invoke(
                orgId,
                agentCode,
                request.getSessionId(),
                request.getContent(),
                request.getRagTagsJson()
        );
        return Result.success(result);
    }

    /**
     * 获取当前请求的目标 orgId。
     *
     * 说明：未注入 OrgContext 时默认使用 ROOT org（1）。
     *
     * @return orgId
     */
    private Long currentOrgId() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId != null ? orgId : 1L;
    }
}
