package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigQueryRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.model.ModelConfigRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigResponse;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.ModelConfigPageQuery;
import com.xbk.knowledge.application.service.ModelConfigAppService;
import com.xbk.knowledge.trigger.converter.DTOConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型配置管理 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigAppService modelConfigAppService;

    /**
     * 查询所有可用模型（分页）
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @PostMapping("/list")
    public Result<PageResult<ModelConfigResponse>> listModels(@Valid @RequestBody ModelConfigQueryRequest request) {
        // 调用应用服务查询
        ModelConfigPageQuery query = new ModelConfigPageQuery(
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<ModelConfig> pageResult = modelConfigAppService.queryModelConfigPage(query);

        // 转换为响应 DTO
        List<ModelConfigResponse> records = pageResult.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<ModelConfigResponse> result = PageResult.of(
                records,
                pageResult.getTotal(),
                pageResult.getPageNum(),
                pageResult.getPageSize()
        );

        return Result.success(result);
    }

    /**
     * 根据 ID 查询模型配置
     *
     * @param request ID 查询请求
     * @return 模型配置
     */
    @PostMapping("/get")
    public Result<ModelConfigResponse> getModel(@Valid @RequestBody IdRequest request) {
        // 调用应用服务查询
        ModelConfig modelConfig = modelConfigAppService.queryModelConfigById(new IdQuery(request.getId()));

        // 转换为响应 DTO
        return Result.success(convertToResponse(modelConfig));
    }

    /**
     * 创建模型配置
     *
     * @param request 模型配置请求
     * @return 创建的模型配置
     */
    @PostMapping("/create")
    public Result<ModelConfigResponse> createModel(@Valid @RequestBody ModelConfigRequest request) {
        // 构建领域实体
        ModelConfig modelConfig = buildModelConfigFromRequest(request);

        // 调用应用服务创建
        ModelConfig savedModel = modelConfigAppService.createModelConfig(modelConfig);

        // 转换为响应 DTO
        return Result.success("模型配置创建成功", convertToResponse(savedModel));
    }

    /**
     * 更新模型配置
     *
     * @param request 模型配置请求（包含 ID）
     * @return 更新后的模型配置
     */
    @PostMapping("/update")
    public Result<ModelConfigResponse> updateModel(@Valid @RequestBody ModelConfigRequest request) {
        // 构建领域实体
        ModelConfig modelConfig = buildModelConfigFromRequest(request);
        modelConfig.setId(request.getId());

        // 调用应用服务更新
        ModelConfig updatedModel = modelConfigAppService.updateModelConfig(modelConfig);

        // 转换为响应 DTO
        return Result.success("模型配置更新成功", convertToResponse(updatedModel));
    }

    /**
     * 删除模型配置
     *
     * @param request ID 查询请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public Result<Void> deleteModel(@Valid @RequestBody IdRequest request) {
        // 调用应用服务删除
        modelConfigAppService.deleteModelConfig(new IdQuery(request.getId()));

        return Result.success();
    }

    /**
     * 启用模型
     *
     * @param request ID 查询请求
     * @return 操作结果
     */
    @PostMapping("/enable")
    public Result<ModelConfigResponse> enableModel(@Valid @RequestBody IdRequest request) {
        // 调用应用服务启用
        ModelConfig updatedModel = modelConfigAppService.enableModel(new IdQuery(request.getId()));

        // 转换为响应 DTO
        return Result.success("模型启用成功", convertToResponse(updatedModel));
    }

    /**
     * 禁用模型
     *
     * @param request ID 查询请求
     * @return 操作结果
     */
    @PostMapping("/disable")
    public Result<ModelConfigResponse> disableModel(@Valid @RequestBody IdRequest request) {
        // 调用应用服务禁用
        ModelConfig updatedModel = modelConfigAppService.disableModel(new IdQuery(request.getId()));

        // 转换为响应 DTO
        return Result.success("模型禁用成功", convertToResponse(updatedModel));
    }

    /**
     * 转换为响应 DTO
     *
     * @param modelConfig 模型配置实体
     * @return 响应 DTO
     */
    private ModelConfigResponse convertToResponse(ModelConfig modelConfig) {
        return ModelConfigResponse.builder()
                .id(modelConfig.getId())
                .modelName(modelConfig.getModelName())
                .modelType(modelConfig.getModelType())
                .baseUrl(modelConfig.getBaseUrl())
                .enabled(modelConfig.getEnabled())
                .priority(modelConfig.getPriority())
                .capability(DTOConverter.toApiModelCapability(modelConfig.getCapability()))
                .createdAt(modelConfig.getCreatedAt())
                .updatedAt(modelConfig.getUpdatedAt())
                .build();
    }

    /**
     * 从请求 DTO 构建领域实体
     *
     * @param request 请求 DTO
     * @return 领域实体
     */
    private ModelConfig buildModelConfigFromRequest(ModelConfigRequest request) {
        ModelConfig modelConfig = ModelConfig.builder()
                .modelName(request.getModelName())
                .modelType(request.getModelType())
                .apiKey(request.getApiKey())
                .baseUrl(request.getBaseUrl())
                .enabled(request.getEnabled())
                .priority(request.getPriority())
                .build();

        // 如果有能力配置，创建关联
        if (request.getCapability() != null) {
            ModelCapability capability = ModelCapability.builder()
                    .modelConfig(modelConfig)
                    .maxInputTokens(request.getCapability().getMaxTokens())
                    .maxOutputTokens(request.getCapability().getMaxTokens())
                    .supportFunctionCalling(false)
                    .supportVision(false)
                    .supportStreaming(false)
                    .qualityScore(request.getCapability().getQualityScore())
                    .build();
            modelConfig.setCapability(capability);
        }

        return modelConfig;
    }
}
