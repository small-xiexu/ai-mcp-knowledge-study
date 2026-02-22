package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IAgentVersionService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.agent.AgentVersionDraftRequest;
import com.xbk.knowledge.api.dto.agent.AgentVersionPublishRequest;
import com.xbk.knowledge.api.dto.agent.AgentVersionQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentVersionResponse;
import com.xbk.knowledge.api.dto.agent.AgentVersionRollbackRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.application.service.app.AgentAppService;
import com.xbk.knowledge.application.service.app.AgentVersionAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.math.BigDecimal;

/**
 * AgentVersion 控制面接口。
 *
 * 职责：HTTP 接口适配，用于转发应用层能力（草稿管理/发布/回滚/查询）。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/agent-versions")
@RequiredArgsConstructor
public class AgentVersionController implements IAgentVersionService {

    private final AgentVersionAppService agentVersionAppService;
    private final AgentAppService agentAppService;
    private final IdentityContextService identityContextService;

    /**
     * 分页查询指定 Agent 的版本列表。
     *
     * @param request 查询请求
     * @return 版本分页
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    @Override
    public Result<PageResult<AgentVersionResponse>> list(@Valid @RequestBody AgentVersionQueryRequest request) {
        Agent agent = agentAppService.queryByCode(new AgentCodeQuery(request.getAgentCode()));
        AgentVersionPageQuery query = new AgentVersionPageQuery(agent.getId(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<AgentVersion> page = agentVersionAppService.queryPage(query);
        PageResult<AgentVersionResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * 根据版本 ID 查询版本详情。
     *
     * @param request 版本 ID 请求
     * @return 版本详情
     */
    @PostMapping("/get")
    @SaCheckPermission("agent:read")
    @Override
    public Result<AgentVersionResponse> get(@Valid @RequestBody IdRequest request) {
        AgentVersion version = agentVersionAppService.queryById(new AgentVersionIdQuery(request.getId()));
        return Result.success(toResponse(version));
    }

    /**
     * 创建或更新草稿版本。
     *
     * 说明：当 id 为空时创建草稿；否则更新草稿（仅 DRAFT 可更新）。
     *
     * @param request 草稿请求
     * @return 保存后的草稿
     */
    @PostMapping("/draft/save")
    @SaCheckPermission("agent:write")
    @Override
    public Result<AgentVersionResponse> saveDraft(@Valid @RequestBody AgentVersionDraftRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        Agent agent = agentAppService.queryByCode(new AgentCodeQuery(request.getAgentCode()));
        AgentVersion draft = AgentVersion.builder()
                .id(request.getId())
                .agentId(agent.getId())
                .versionNo(request.getVersionNo())
                .state("DRAFT")
                .changeSummary(request.getChangeSummary())
                .promptTemplateId(request.getPromptTemplateId())
                .templateParamsJson(request.getTemplateParamsJson())
                .workflowVersionId(request.getWorkflowVersionId())
                .ragMode(request.getRagMode())
                .defaultRagTagsJson(request.getDefaultRagTagsJson())
                .allowedRagTagsJson(request.getAllowedRagTagsJson())
                .allowedToolKeysJson(request.getAllowedToolKeysJson())
                .clientProfileId(request.getClientProfileId())
                .clientChainJson(request.getClientChainJson())
                .outputContractVersion(request.getOutputContractVersion())
                .outputContractOptionsJson(request.getOutputContractOptionsJson())
                .timeoutMs(request.getTimeoutMs())
                .maxTurns(request.getMaxTurns())
                .temperature(request.getTemperature() == null ? null : BigDecimal.valueOf(request.getTemperature()))
                .repairRetryTimes(request.getRepairRetryTimes())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        AgentVersion saved;
        if (request.getId() == null) {
            saved = agentVersionAppService.createDraft(draft);
            return Result.success("草稿创建成功", toResponse(saved));
        }
        saved = agentVersionAppService.updateDraft(draft);
        return Result.success("草稿更新成功", toResponse(saved));
    }

    /**
     * 发布指定版本（切换为当前生效版本）。
     *
     * @param request 发布请求
     * @return 发布后的版本
     */
    @PostMapping("/publish")
    @SaCheckPermission("agent:publish")
    @Override
    public Result<AgentVersionResponse> publish(@Valid @RequestBody AgentVersionPublishRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        AgentVersion published = agentVersionAppService.publish(request.getAgentCode(), request.getVersionId(), userId);
        return Result.success("发布成功", toResponse(published));
    }

    /**
     * 回滚到指定历史版本（切换当前生效版本）。
     *
     * @param request 回滚请求
     * @return 回滚后的目标版本
     */
    @PostMapping("/rollback")
    @SaCheckPermission("agent:publish")
    @Override
    public Result<AgentVersionResponse> rollback(@Valid @RequestBody AgentVersionRollbackRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        AgentVersion target = agentVersionAppService.rollback(request.getAgentCode(), request.getTargetVersionId(), userId);
        return Result.success("回滚成功", toResponse(target));
    }

    /**
     * 领域对象转输出 DTO。
     *
     * @param v AgentVersion 实体
     * @return 响应 DTO
     */
    private AgentVersionResponse toResponse(AgentVersion v) {
        Double temperature = v.getTemperature() == null ? null : v.getTemperature().doubleValue();
        return AgentVersionResponse.builder()
                .id(v.getId())
                .agentId(v.getAgentId())
                .versionNo(v.getVersionNo())
                .state(v.getState())
                .changeSummary(v.getChangeSummary())
                .promptTemplateId(v.getPromptTemplateId())
                .promptTemplateVersionNo(v.getPromptTemplateVersionNo())
                .templateParamsJson(v.getTemplateParamsJson())
                .systemPromptSnapshot(v.getSystemPromptSnapshot())
                .workflowVersionId(v.getWorkflowVersionId())
                .outputContractVersion(v.getOutputContractVersion())
                .outputContractOptionsJson(v.getOutputContractOptionsJson())
                .ragMode(v.getRagMode())
                .defaultRagTagsJson(v.getDefaultRagTagsJson())
                .allowedRagTagsJson(v.getAllowedRagTagsJson())
                .allowedToolKeysJson(v.getAllowedToolKeysJson())
                .clientProfileId(v.getClientProfileId())
                .clientChainJson(v.getClientChainJson())
                .timeoutMs(v.getTimeoutMs())
                .maxTurns(v.getMaxTurns())
                .temperature(temperature)
                .repairRetryTimes(v.getRepairRetryTimes())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

}
