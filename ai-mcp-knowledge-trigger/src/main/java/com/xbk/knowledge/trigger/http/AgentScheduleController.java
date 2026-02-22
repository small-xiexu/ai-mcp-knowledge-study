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
