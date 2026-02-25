package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IAgentService;
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
import com.xbk.knowledge.types.common.PageQueryExecutor;
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
public class AgentController implements IAgentService {
    /**
     * Agent 应用服务，用于 Agent 的创建、更新、删除与查询。
     */
    private final AgentAppService agentAppService;

    /**
     * 身份上下文服务，用于获取当前登录用户 ID。
     */
    private final IdentityContextService identityContextService;

    /**
     * 分页查询 Agent 列表。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `AgentPageQuery` 并调用 `agentAppService.queryPage`。
     * 4. 将领域分页结果转换为 `AgentResponse` 分页结果。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 查询条件
     * @return PageResult<AgentResponse> 分页结果。
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    @Override
    public Result<PageResult<AgentResponse>> list(@Valid @RequestBody AgentQueryRequest request) {
        return PageQueryExecutor.execute(
                request,
                (offset, pageSize) -> new AgentPageQuery(
                        request.getKeyword(),
                        request.getStatus(),
                        offset,
                        pageSize
                ),
                agentAppService::queryPage,
                this::toResponse
        );
    }

    /**
     * 根据 agentCode 查询 Agent 详情。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `AgentCodeQuery` 并调用 `agentAppService.queryByCode`。
     * 4. 将领域实体转换为 `AgentResponse`。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request agentCode 请求
     * @return Agent 详情
     */
    @PostMapping("/get")
    @SaCheckPermission("agent:read")
    @Override
    public Result<AgentResponse> get(@Valid @RequestBody AgentCodeRequest request) {
        Agent agent = agentAppService.queryByCode(new AgentCodeQuery(request.getAgentCode()));
        return Result.success(toResponse(agent));
    }

    /**
     * 创建 Agent。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 从登录上下文获取当前用户并组装 `Agent` 领域对象。
     * 4. 调用 `agentAppService.create` 落库创建 Agent。
     * 5. 将创建结果转换为 `AgentResponse` 并统一封装返回。
     * 
     * @param request 创建参数
     * @return 创建后的 Agent
     */
    @PostMapping("/create")
    @SaCheckPermission("agent:write")
    @Override
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
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 从登录上下文获取当前用户并组装更新领域对象。
     * 4. 调用 `agentAppService.update` 执行更新。
     * 5. 将更新结果转换为 `AgentResponse` 并统一封装返回。
     * 
     * @param request 更新参数
     * @return 更新后的 Agent
     */
    @PostMapping("/update")
    @SaCheckPermission("agent:write")
    @Override
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
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `AgentCodeQuery` 并调用 `agentAppService.remove`。
     * 4. 应用层执行 Agent 及关联配置/运行数据的级联删除。
     * 5. 统一封装空成功结果返回。
     * 
     * @param request 删除参数
     * @return 空结果
     */
    @PostMapping("/remove")
    @SaCheckPermission("agent:write")
    @Override
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
