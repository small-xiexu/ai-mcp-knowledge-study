package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IAgentScheduleService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.agent.AgentScheduleCreateRequest;
import com.xbk.knowledge.api.dto.agent.AgentScheduleQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentScheduleResponse;
import com.xbk.knowledge.api.dto.agent.AgentScheduleUpdateRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.application.service.app.AgentScheduleAppService;
import com.xbk.knowledge.application.service.app.AgentAppService;
import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * AgentSchedule 控制面接口。
 *
 * 职责：HTTP 接口适配，用于转发应用层能力（调度配置 CRUD/启停）。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class AgentScheduleController implements IAgentScheduleService {

    private final AgentScheduleAppService agentScheduleAppService;
    private final AgentAppService agentAppService;

    /**
     * 分页查询调度列表。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. 若传入 `agentCode`，先查询 Agent 再得到 `agentId`。
     * 4. 组装 `AgentSchedulePageQuery` 并调用 `agentScheduleAppService.queryPage`。
     * 5. 转换分页结果并统一封装 `Result.success` 返回。
     *
     * @param request 分页查询参数
     * @return 调度分页结果
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    @Override
    public Result<PageResult<AgentScheduleResponse>> list(@Valid @RequestBody AgentScheduleQueryRequest request) {
        Long agentId = null;
        if (StringUtils.hasText(request.getAgentCode())) {
            Agent agent = agentAppService.queryByCode(new AgentCodeQuery(request.getAgentCode()));
            agentId = agent.getId();
        }
        AgentSchedulePageQuery query = new AgentSchedulePageQuery(agentId,
                request.getScheduleName(),
                request.getEnabled(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<AgentSchedule> page = agentScheduleAppService.queryPage(query);
        PageResult<AgentScheduleResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * 查询调度详情。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `AgentScheduleIdQuery` 并调用 `agentScheduleAppService.queryById`。
     * 4. 将领域实体转换为 `AgentScheduleResponse`。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 调度 ID 请求
     * @return 调度详情
     */
    @PostMapping("/get")
    @SaCheckPermission("agent:read")
    @Override
    public Result<AgentScheduleResponse> get(@Valid @RequestBody IdRequest request) {
        AgentSchedule schedule = agentScheduleAppService.queryById(new AgentScheduleIdQuery(request.getId()));
        return Result.success(toResponse(schedule));
    }

    /**
     * 创建调度。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `AgentSchedule` 领域对象。
     * 4. 调用 `agentScheduleAppService.create` 创建调度并注册对应任务。
     * 5. 将创建结果转换为 `AgentScheduleResponse` 并统一返回。
     *
     * @param request 创建请求
     * @return 创建后的调度配置
     */
    @PostMapping("/create")
    @SaCheckPermission("agent:write")
    @Override
    public Result<AgentScheduleResponse> create(@Valid @RequestBody AgentScheduleCreateRequest request) {
        AgentSchedule schedule = AgentSchedule.builder()
                .scheduleName(request.getScheduleName())
                .description(request.getDescription())
                .cron(request.getCron())
                .enabled(request.getEnabled())
                .payloadTemplateJson(request.getPayloadTemplateJson())
                .build();
        AgentSchedule created = agentScheduleAppService.create(schedule, request.getAgentCode());
        return Result.success(toResponse(created));
    }

    /**
     * 更新调度。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装更新对象并调用 `agentScheduleAppService.update`。
     * 4. 应用层完成调度配置更新与任务同步。
     * 5. 将更新结果转换为 `AgentScheduleResponse` 并统一返回。
     *
     * @param request 更新请求
     * @return 更新后的调度配置
     */
    @PostMapping("/update")
    @SaCheckPermission("agent:write")
    @Override
    public Result<AgentScheduleResponse> update(@Valid @RequestBody AgentScheduleUpdateRequest request) {
        AgentSchedule schedule = AgentSchedule.builder()
                .id(request.getId())
                .scheduleName(request.getScheduleName())
                .description(request.getDescription())
                .cron(request.getCron())
                .payloadTemplateJson(request.getPayloadTemplateJson())
                .build();
        AgentSchedule updated = agentScheduleAppService.update(schedule, request.getAgentCode());
        return Result.success(toResponse(updated));
    }

    /**
     * 启用调度。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `agentScheduleAppService.enable` 启用调度。
     * 4. 应用层完成任务状态切换。
     * 5. 返回启用后的 `AgentScheduleResponse`。
     *
     * @param request 调度 ID 请求
     * @return 启用后的调度配置
     */
    @PostMapping("/enable")
    @SaCheckPermission("agent:write")
    @Override
    public Result<AgentScheduleResponse> enable(@Valid @RequestBody IdRequest request) {
        AgentSchedule schedule = agentScheduleAppService.enable(request.getId());
        return Result.success(toResponse(schedule));
    }

    /**
     * 禁用调度。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `agentScheduleAppService.disable` 禁用调度。
     * 4. 应用层完成任务状态切换。
     * 5. 返回禁用后的 `AgentScheduleResponse`。
     *
     * @param request 调度 ID 请求
     * @return 禁用后的调度配置
     */
    @PostMapping("/disable")
    @SaCheckPermission("agent:write")
    @Override
    public Result<AgentScheduleResponse> disable(@Valid @RequestBody IdRequest request) {
        AgentSchedule schedule = agentScheduleAppService.disable(request.getId());
        return Result.success(toResponse(schedule));
    }

    /**
     * 删除调度。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `agentScheduleAppService.remove` 执行删除。
     * 4. 应用层完成调度与关联任务清理。
     * 5. 统一返回空成功结果。
     *
     * @param request 调度 ID 请求
     * @return 空成功结果
     */
    @PostMapping("/remove")
    @SaCheckPermission("agent:write")
    @Override
    public Result<Void> remove(@Valid @RequestBody IdRequest request) {
        agentScheduleAppService.remove(request.getId());
        return Result.success();
    }

    private AgentScheduleResponse toResponse(AgentSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return AgentScheduleResponse.builder()
                .id(schedule.getId())
                .agentId(schedule.getAgentId())
                .agentCode(schedule.getAgentCode())
                .scheduleName(schedule.getScheduleName())
                .description(schedule.getDescription())
                .cron(schedule.getCron())
                .enabled(schedule.getEnabled())
                .xxlJobId(schedule.getXxlJobId())
                .payloadTemplateJson(schedule.getPayloadTemplateJson())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }

}
