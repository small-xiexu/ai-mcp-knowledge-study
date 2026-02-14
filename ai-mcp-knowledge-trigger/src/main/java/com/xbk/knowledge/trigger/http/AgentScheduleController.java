package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.agent.AgentScheduleCreateRequest;
import com.xbk.knowledge.api.dto.agent.AgentScheduleQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentScheduleResponse;
import com.xbk.knowledge.api.dto.agent.AgentScheduleUpdateRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.application.service.app.AgentScheduleAppService;
import com.xbk.knowledge.application.service.app.AgentAppService;
import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.entity.agent.AgentSchedule;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentSchedulePageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.context.OrgContextHolder;
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
 * @author xiexu
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class AgentScheduleController {

    private final AgentScheduleAppService agentScheduleAppService;
    private final AgentAppService agentAppService;

    /**
     * 分页查询调度列表。
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    public Result<PageResult<AgentScheduleResponse>> list(@Valid @RequestBody AgentScheduleQueryRequest request) {
        Long orgId = currentOrgId();
        Long agentId = null;
        if (StringUtils.hasText(request.getAgentCode())) {
            Agent agent = agentAppService.queryByCode(new AgentCodeQuery(orgId, request.getAgentCode()));
            agentId = agent.getId();
        }
        AgentSchedulePageQuery query = new AgentSchedulePageQuery(
                orgId,
                agentId,
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
    public Result<AgentScheduleResponse> get(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        AgentSchedule schedule = agentScheduleAppService.queryById(new AgentScheduleIdQuery(orgId, request.getId()));
        return Result.success(toResponse(schedule));
    }

    /**
     * 创建调度。
     */
    @PostMapping("/create")
    @SaCheckPermission("agent:write")
    public Result<AgentScheduleResponse> create(@Valid @RequestBody AgentScheduleCreateRequest request) {
        Long orgId = currentOrgId();
        AgentSchedule schedule = AgentSchedule.builder()
                .orgId(orgId)
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
    public Result<AgentScheduleResponse> update(@Valid @RequestBody AgentScheduleUpdateRequest request) {
        Long orgId = currentOrgId();
        AgentSchedule schedule = AgentSchedule.builder()
                .id(request.getId())
                .orgId(orgId)
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
    public Result<AgentScheduleResponse> enable(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        AgentSchedule schedule = agentScheduleAppService.enable(orgId, request.getId());
        return Result.success(toResponse(schedule));
    }

    /**
     * 禁用调度。
     */
    @PostMapping("/disable")
    @SaCheckPermission("agent:write")
    public Result<AgentScheduleResponse> disable(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        AgentSchedule schedule = agentScheduleAppService.disable(orgId, request.getId());
        return Result.success(toResponse(schedule));
    }

    /**
     * 删除调度。
     */
    @PostMapping("/remove")
    @SaCheckPermission("agent:write")
    public Result<Void> remove(@Valid @RequestBody IdRequest request) {
        Long orgId = currentOrgId();
        agentScheduleAppService.remove(orgId, request.getId());
        return Result.success();
    }

    private AgentScheduleResponse toResponse(AgentSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return AgentScheduleResponse.builder()
                .id(schedule.getId())
                .orgId(schedule.getOrgId())
                .agentId(schedule.getAgentId())
                .agentCode(schedule.getAgentCode())
                .cron(schedule.getCron())
                .enabled(schedule.getEnabled())
                .xxlJobId(schedule.getXxlJobId())
                .payloadTemplateJson(schedule.getPayloadTemplateJson())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }

    private Long currentOrgId() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId != null ? orgId : 1L;
    }
}

