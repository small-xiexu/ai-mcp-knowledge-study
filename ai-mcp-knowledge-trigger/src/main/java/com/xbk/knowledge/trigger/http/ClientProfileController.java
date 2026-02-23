package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IClientProfileService;
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
public class ClientProfileController implements IClientProfileService {

    private final ClientProfileAppService clientProfileAppService;
    private final IdentityContextService identityContextService;

    /**
     * 根据筛选条件查询客户端画像列表。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `ClientProfilePageQuery` 并调用 `clientProfileAppService.queryPage`。
     * 4. 将领域分页结果转换为 `ClientProfileResponse`（列表接口不展开 steps）。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 客户端画像分页查询参数。
     * @return 返回 ClientProfileResponse 分页数据。
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    @Override
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
     * 查询客户端画像。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `clientProfileAppService.get` 查询主档。
     * 4. 再调用 `clientProfileAppService.listSteps` 查询步骤链。
     * 5. 聚合为 `ClientProfileResponse` 并统一返回。
     *
     * @param request 客户端画像查询参数。
     * @return 返回 ClientProfileResponse 数据。
     */
    @PostMapping("/get")
    @SaCheckPermission("agent:read")
    @Override
    public Result<ClientProfileResponse> get(@Valid @RequestBody IdRequest request) {
        ClientProfile profile = clientProfileAppService.get(request.getId());
        List<ClientProfileStep> steps = clientProfileAppService.listSteps(request.getId());
        return Result.success(toResponse(profile, steps));
    }

    /**
     * 创建或更新客户端画像数据。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 从登录上下文获取用户并组装 `ClientProfile` + `ClientProfileStep`。
     * 4. 调用 `clientProfileAppService.save` 执行保存。
     * 5. 保存后回查步骤链并组装完整响应返回。
     *
     * @param request 客户端画像保存参数。
     * @return 返回 ClientProfileResponse 数据。
     */
    @PostMapping("/save")
    @SaCheckPermission("agent:write")
    @Override
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
     * 启用业务配置。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 获取当前用户并调用 `clientProfileAppService.enable`。
     * 4. 回查步骤链并组装启用后的完整响应。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 客户端画像启停参数。
     * @return 返回 ClientProfileResponse 数据。
     */
    @PostMapping("/enable")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ClientProfileResponse> enable(@Valid @RequestBody IdRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        ClientProfile enabled = clientProfileAppService.enable(request.getId(), userId);
        List<ClientProfileStep> steps = clientProfileAppService.listSteps(enabled.getId());
        return Result.success(toResponse(enabled, steps));
    }

    /**
     * 禁用业务配置。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 获取当前用户并调用 `clientProfileAppService.disable`。
     * 4. 回查步骤链并组装禁用后的完整响应。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 客户端画像启停参数。
     * @return 返回 ClientProfileResponse 数据。
     */
    @PostMapping("/disable")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ClientProfileResponse> disable(@Valid @RequestBody IdRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        ClientProfile disabled = clientProfileAppService.disable(request.getId(), userId);
        List<ClientProfileStep> steps = clientProfileAppService.listSteps(disabled.getId());
        return Result.success(toResponse(disabled, steps));
    }

    /**
     * 删除客户端画像数据。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `clientProfileAppService.remove` 执行删除。
     * 4. 应用层负责清理主档与关联步骤数据。
     * 5. 统一返回空成功结果。
     *
     * @param request 客户端画像删除参数。
     * @return 返回客户端画像删除状态。
     */
    @PostMapping("/remove")
    @SaCheckPermission("agent:write")
    @Override
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
