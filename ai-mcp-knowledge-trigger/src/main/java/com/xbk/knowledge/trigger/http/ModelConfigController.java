package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigQueryRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.model.ModelConfigRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigResponse;
import com.xbk.knowledge.api.dto.model.ModelCapabilityRequest;
import com.xbk.knowledge.api.dto.model.ModelCapabilityDTO;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.trigger.converter.DTOConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

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
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        ModelConfigPageQuery query = new ModelConfigPageQuery(
                offset,
                pageSize
        );
        PageResult<ModelConfig> pageResult = modelConfigAppService.queryModelConfigPage(query);

        // 转换为响应 DTO 并构建分页结果
        PageResult<ModelConfigResponse> result = PageResultConverter.convert(pageResult, this::convertToResponse);

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
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig modelConfig = modelConfigAppService.queryModelConfigById(idQuery);

        // 转换为响应 DTO
        ModelConfigResponse response = convertToResponse(modelConfig);
        return Result.success(response);
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
        ModelConfigResponse response = convertToResponse(savedModel);
        return Result.success("模型配置创建成功", response);
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
        Long id = request.getId();
        modelConfig.setId(id);

        // 调用应用服务更新
        ModelConfig updatedModel = modelConfigAppService.updateModelConfig(modelConfig);

        // 转换为响应 DTO
        ModelConfigResponse response = convertToResponse(updatedModel);
        return Result.success("模型配置更新成功", response);
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
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        modelConfigAppService.deleteModelConfig(idQuery);

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
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig updatedModel = modelConfigAppService.enableModel(idQuery);

        // 转换为响应 DTO
        ModelConfigResponse response = convertToResponse(updatedModel);
        return Result.success("模型启用成功", response);
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
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig updatedModel = modelConfigAppService.disableModel(idQuery);

        // 转换为响应 DTO
        ModelConfigResponse response = convertToResponse(updatedModel);
        return Result.success("模型禁用成功", response);
    }

    /**
     * 转换为响应 DTO
     *
     * @param modelConfig 模型配置实体
     * @return 响应 DTO
     */
    private ModelConfigResponse convertToResponse(ModelConfig modelConfig) {
        Long modelId = modelConfig.getId();
        String modelName = modelConfig.getModelName();
        ModelType modelType = modelConfig.getModelType();
        String baseUrl = modelConfig.getBaseUrl();
        Boolean enabled = modelConfig.getEnabled();
        Integer priority = modelConfig.getPriority();
        ModelCapability modelCapability = modelConfig.getCapability();
        ModelCapabilityDTO capability = DTOConverter.toApiModelCapability(modelCapability);
        LocalDateTime createdAt = modelConfig.getCreatedAt();
        LocalDateTime updatedAt = modelConfig.getUpdatedAt();
        return ModelConfigResponse.builder()
                .id(modelId)
                .modelName(modelName)
                .modelType(modelType)
                .baseUrl(baseUrl)
                .enabled(enabled)
                .priority(priority)
                .capability(capability)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * 从请求 DTO 构建领域实体
     *
     * @param request 请求 DTO
     * @return 领域实体
     */
    private ModelConfig buildModelConfigFromRequest(ModelConfigRequest request) {
        String modelName = request.getModelName();
        ModelType modelType = request.getModelType();
        String apiKey = request.getApiKey();
        String baseUrl = request.getBaseUrl();
        Boolean enabled = request.getEnabled();
        Integer priority = request.getPriority();
        ModelConfig modelConfig = ModelConfig.builder()
                .modelName(modelName)
                .modelType(modelType)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .enabled(enabled)
                .priority(priority)
                .build();

        // 如果有能力配置，创建关联
        if (request.getCapability() != null) {
            ModelCapabilityRequest capabilityRequest = request.getCapability();
            Integer maxTokens = capabilityRequest.getMaxTokens();
            Integer qualityScore = capabilityRequest.getQualityScore();
            ModelCapability capability = ModelCapability.builder()
                    .modelConfig(modelConfig)
                    .maxInputTokens(maxTokens)
                    .maxOutputTokens(maxTokens)
                    .supportFunctionCalling(false)
                    .supportVision(false)
                    .supportStreaming(false)
                    .qualityScore(qualityScore)
                    .build();
            modelConfig.setCapability(capability);
        }

        return modelConfig;
    }
}
