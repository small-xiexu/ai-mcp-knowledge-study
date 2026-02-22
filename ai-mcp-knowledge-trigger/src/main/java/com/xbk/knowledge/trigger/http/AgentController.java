package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.agent.AgentCodeRequest;
import com.xbk.knowledge.api.dto.agent.AgentCreateRequest;
import com.xbk.knowledge.api.dto.agent.AgentQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentResponse;
import com.xbk.knowledge.api.dto.agent.AgentUpdateRequest;
import com.xbk.knowledge.application.service.app.AgentAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Agent 控制面接口。
 *
 * 职责：HTTP 接口适配，用于转发应用层能力（Agent 创建/更新/查询）。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentAppService agentAppService;
    private final IdentityContextService identityContextService;

    /**
     * 分页查询 Agent 列表。
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    public Result<PageResult<AgentResponse>> list(@Valid @RequestBody AgentQueryRequest request) {
        AgentPageQuery query = new AgentPageQuery(request.getKeyword(),
                request.getStatus(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<Agent> page = agentAppService.queryPage(query);
        PageResult<AgentResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * 根据 agentCode 查询 Agent 详情。
     *
     * @param request agentCode 请求
     * @return Agent 详情
     */
    @PostMapping("/get")
    @SaCheckPermission("agent:read")
    public Result<AgentResponse> get(@Valid @RequestBody AgentCodeRequest request) {
        Agent agent = agentAppService.queryByCode(new AgentCodeQuery(request.getAgentCode()));
        return Result.success(toResponse(agent));
    }

    /**
     * 创建 Agent。
     *
     * @param request 创建请求
     * @return 创建后的 Agent
     */
    @PostMapping("/create")
    @SaCheckPermission("agent:write")
    public Result<AgentResponse> create(@Valid @RequestBody AgentCreateRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        Agent agent = Agent.builder()
                .agentCode(request.getAgentCode())
                .agentName(request.getAgentName())
                .description(request.getDescription())
                .channel(request.getChannel())
                .status(request.getStatus())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        Agent saved = agentAppService.create(agent);
        return Result.success("Agent 创建成功", toResponse(saved));
    }

    /**
     * 更新 Agent（按 agentCode 定位）。
     *
     * @param request 更新请求
     * @return 更新后的 Agent
     */
    @PostMapping("/update")
    @SaCheckPermission("agent:write")
    public Result<AgentResponse> update(@Valid @RequestBody AgentUpdateRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        Agent agent = Agent.builder()
                .agentCode(request.getAgentCode())
                .agentName(request.getAgentName())
                .description(request.getDescription())
                .channel(request.getChannel())
                .status(request.getStatus())
                .updatedBy(userId)
                .build();
        Agent updated = agentAppService.update(agent);
        return Result.success("Agent 更新成功", toResponse(updated));
    }

    /**
     * 删除 Agent 及其关联数据。
     *
     * @param request 删除请求
     * @return 空结果
     */
    @PostMapping("/remove")
    @SaCheckPermission("agent:write")
    public Result<Void> remove(@Valid @RequestBody AgentCodeRequest request) {
        agentAppService.remove(new AgentCodeQuery(request.getAgentCode()));
        return Result.success();
    }

    /**
     * 领域对象转输出 DTO。
     *
     * @param agent Agent 实体
     * @return 响应 DTO
     */
    private AgentResponse toResponse(Agent agent) {
        return AgentResponse.builder()
                .id(agent.getId())
                .agentCode(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .channel(agent.getChannel())
                .status(agent.getStatus())
                .currentPublishedVersionId(agent.getCurrentPublishedVersionId())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

}
