package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.client.ClientProfileQueryRequest;
import com.xbk.knowledge.api.dto.client.ClientProfileResponse;
import com.xbk.knowledge.api.dto.client.ClientProfileSaveRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.application.service.app.ClientProfileAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.domain.client.model.entity.ClientProfile;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.client.model.valobj.ClientProfilePageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Client Profile 控制面接口。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/client-profiles")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileAppService clientProfileAppService;
    private final IdentityContextService identityContextService;

    /**
     * list。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    public Result<PageResult<ClientProfileResponse>> list(@Valid @RequestBody ClientProfileQueryRequest request) {
        ClientProfilePageQuery query = ClientProfilePageQuery.builder()
                .keyword(request.getKeyword())
                .status(request.getStatus())
                .offset(request.getOffset())
                .pageSize(request.getPageSize())
                .build();
        PageResult<ClientProfile> page = clientProfileAppService.queryPage(query);
        PageResult<ClientProfileResponse> resp = PageResultConverter.convert(page, this::toResponseWithoutSteps);
        return Result.success(resp);
    }

    /**
     * get。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/get")
    @SaCheckPermission("agent:read")
    public Result<ClientProfileResponse> get(@Valid @RequestBody IdRequest request) {
        ClientProfile profile = clientProfileAppService.get(request.getId());
        List<ClientProfileStep> steps = clientProfileAppService.listSteps(request.getId());
        return Result.success(toResponse(profile, steps));
    }

    /**
     * save。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/save")
    @SaCheckPermission("agent:write")
    public Result<ClientProfileResponse> save(@Valid @RequestBody ClientProfileSaveRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        ClientProfile profile = ClientProfile.builder()
                .id(request.getId())
                .clientCode(request.getClientCode())
                .clientName(request.getClientName())
                .description(request.getDescription())
                .status(request.getStatus())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        List<ClientProfileStep> steps = toSteps(request.getSteps());
        ClientProfile saved = clientProfileAppService.save(profile, steps);
        List<ClientProfileStep> savedSteps = clientProfileAppService.listSteps(saved.getId());
        return Result.success("保存成功", toResponse(saved, savedSteps));
    }

    /**
     * enable。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/enable")
    @SaCheckPermission("agent:write")
    public Result<ClientProfileResponse> enable(@Valid @RequestBody IdRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        ClientProfile enabled = clientProfileAppService.enable(request.getId(), userId);
        List<ClientProfileStep> steps = clientProfileAppService.listSteps(enabled.getId());
        return Result.success(toResponse(enabled, steps));
    }

    /**
     * disable。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/disable")
    @SaCheckPermission("agent:write")
    public Result<ClientProfileResponse> disable(@Valid @RequestBody IdRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        ClientProfile disabled = clientProfileAppService.disable(request.getId(), userId);
        List<ClientProfileStep> steps = clientProfileAppService.listSteps(disabled.getId());
        return Result.success(toResponse(disabled, steps));
    }

    /**
     * remove。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/remove")
    @SaCheckPermission("agent:write")
    public Result<Void> remove(@Valid @RequestBody IdRequest request) {
        clientProfileAppService.remove(request.getId());
        return Result.success();
    }

    private List<ClientProfileStep> toSteps(List<ClientProfileSaveRequest.ClientProfileStepItem> requestSteps) {
        if (requestSteps == null || requestSteps.isEmpty()) {
            return List.of();
        }
        List<ClientProfileStep> steps = new ArrayList<>();
        for (ClientProfileSaveRequest.ClientProfileStepItem item : requestSteps) {
            if (item == null) {
                continue;
            }
            steps.add(ClientProfileStep.builder()
                    .sequenceNo(item.getSequenceNo())
                    .stepName(item.getStepName())
                    .modelId(item.getModelId())
                    .systemPrompt(item.getSystemPrompt())
                    .enableTools(item.getEnableTools())
                    .allowedToolKeysJson(item.getAllowedToolKeysJson())
                    .build());
        }
        return steps;
    }

    private ClientProfileResponse toResponseWithoutSteps(ClientProfile profile) {
        return toResponse(profile, null);
    }

    private ClientProfileResponse toResponse(ClientProfile profile, List<ClientProfileStep> steps) {
        if (profile == null) {
            return null;
        }
        return ClientProfileResponse.builder()
                .id(profile.getId())
                .clientCode(profile.getClientCode())
                .clientName(profile.getClientName())
                .description(profile.getDescription())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .steps(toStepResponses(steps))
                .build();
    }

    private List<ClientProfileResponse.ClientProfileStepResponse> toStepResponses(List<ClientProfileStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        List<ClientProfileResponse.ClientProfileStepResponse> responses = new ArrayList<>(steps.size());
        for (ClientProfileStep step : steps) {
            if (step == null) {
                continue;
            }
            responses.add(ClientProfileResponse.ClientProfileStepResponse.builder()
                    .id(step.getId())
                    .clientProfileId(step.getClientProfileId())
                    .sequenceNo(step.getSequenceNo())
                    .stepName(step.getStepName())
                    .modelId(step.getModelId())
                    .systemPrompt(step.getSystemPrompt())
                    .enableTools(step.getEnableTools())
                    .allowedToolKeysJson(step.getAllowedToolKeysJson())
                    .createdAt(step.getCreatedAt())
                    .updatedAt(step.getUpdatedAt())
                    .build());
        }
        return responses;
    }
}
