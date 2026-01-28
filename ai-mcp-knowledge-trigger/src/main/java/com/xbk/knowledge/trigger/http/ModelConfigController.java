package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.ModelConfigRequest;
import com.xbk.knowledge.api.dto.ModelConfigResponse;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.trigger.converter.DTOConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型配置管理 Controller
 * 提供模型配置的增删改查接口
 *
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigRepository modelConfigRepository;

    /**
     * 查询所有可用模型（分页）
     *
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<ModelConfigResponse>> listModels(PageRequest pageRequest) {
        log.info("查询模型列表，pageNum: {}, pageSize: {}", pageRequest.getPageNum(), pageRequest.getPageSize());

        // 验证并修正分页参数
        pageRequest.validate();

        // 构建 Spring Data 分页请求
        org.springframework.data.domain.PageRequest springPageRequest = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPageNum() - 1,  // Spring Data 页码从 0 开始
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "priority", "createdAt")
        );

        // 查询分页数据
        Page<ModelConfig> page = modelConfigRepository.findAll(springPageRequest);

        // 转换为响应 DTO
        List<ModelConfigResponse> records = page.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<ModelConfigResponse> pageResult = PageResult.of(
                records,
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );

        return Result.success(pageResult);
    }

    /**
     * 根据 ID 查询模型配置
     *
     * @param id 模型 ID
     * @return 模型配置
     */
    @GetMapping("/{id}")
    public Result<ModelConfigResponse> getModel(@PathVariable Long id) {
        log.info("查询模型配置，id: {}", id);

        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));

        return Result.success(convertToResponse(modelConfig));
    }

    /**
     * 创建模型配置
     *
     * @param request 模型配置请求
     * @return 创建的模型配置
     */
    @PostMapping
    public Result<ModelConfigResponse> createModel(@Valid @RequestBody ModelConfigRequest request) {
        log.info("创建模型配置，modelName: {}, modelType: {}", request.getModelName(), request.getModelType());

        // 检查模型名称是否已存在
        if (modelConfigRepository.findByModelName(request.getModelName()).isPresent()) {
            throw new IllegalArgumentException("模型名称已存在：" + request.getModelName());
        }

        // 构建模型配置实体
        ModelConfig modelConfig = ModelConfig.builder()
                .modelName(request.getModelName())
                .modelType(request.getModelType())
                .apiKey(request.getApiKey())
                .baseUrl(request.getBaseUrl())
                .enabled(request.getEnabled())
                .priority(request.getPriority())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            modelConfig.setCapability(capability);
        }

        // 保存到数据库
        ModelConfig savedModel = modelConfigRepository.save(modelConfig);

        log.info("模型配置创建成功，id: {}", savedModel.getId());
        return Result.success("模型配置创建成功", convertToResponse(savedModel));
    }

    /**
     * 更新模型配置
     *
     * @param id      模型 ID
     * @param request 模型配置请求
     * @return 更新后的模型配置
     */
    @PutMapping("/{id}")
    public Result<ModelConfigResponse> updateModel(@PathVariable Long id,
                                                     @Valid @RequestBody ModelConfigRequest request) {
        log.info("更新模型配置，id: {}, modelName: {}", id, request.getModelName());

        // 查询现有配置
        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));

        // 检查模型名称是否与其他模型冲突
        modelConfigRepository.findByModelName(request.getModelName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("模型名称已存在：" + request.getModelName());
                    }
                });

        // 更新字段
        modelConfig.setModelName(request.getModelName());
        modelConfig.setModelType(request.getModelType());
        modelConfig.setApiKey(request.getApiKey());
        modelConfig.setBaseUrl(request.getBaseUrl());
        modelConfig.setEnabled(request.getEnabled());
        modelConfig.setPriority(request.getPriority());
        modelConfig.setUpdatedAt(LocalDateTime.now());

        // 更新能力配置
        if (request.getCapability() != null) {
            ModelCapability capability = modelConfig.getCapability();
            if (capability == null) {
                capability = ModelCapability.builder()
                        .modelConfig(modelConfig)
                        .build();
                modelConfig.setCapability(capability);
            }
            capability.setMaxInputTokens(request.getCapability().getMaxTokens());
            capability.setMaxOutputTokens(request.getCapability().getMaxTokens());
            capability.setQualityScore(request.getCapability().getQualityScore());
            capability.setUpdatedAt(LocalDateTime.now());
        }

        // 保存更新
        ModelConfig updatedModel = modelConfigRepository.save(modelConfig);

        log.info("模型配置更新成功，id: {}", id);
        return Result.success("模型配置更新成功", convertToResponse(updatedModel));
    }

    /**
     * 删除模型配置
     *
     * @param id 模型 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        log.info("删除模型配置，id: {}", id);

        // 检查模型是否存在
        if (!modelConfigRepository.existsById(id)) {
            throw new NotFoundException("模型配置不存在，id: " + id);
        }

        // 删除模型
        modelConfigRepository.deleteById(id);

        log.info("模型配置删除成功，id: {}", id);
        return Result.success();
    }

    /**
     * 启用模型
     *
     * @param id 模型 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/enable")
    public Result<ModelConfigResponse> enableModel(@PathVariable Long id) {
        log.info("启用模型，id: {}", id);

        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));

        modelConfig.setEnabled(true);
        modelConfig.setUpdatedAt(LocalDateTime.now());
        ModelConfig updatedModel = modelConfigRepository.save(modelConfig);

        log.info("模型启用成功，id: {}", id);
        return Result.success("模型启用成功", convertToResponse(updatedModel));
    }

    /**
     * 禁用模型
     *
     * @param id 模型 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/disable")
    public Result<ModelConfigResponse> disableModel(@PathVariable Long id) {
        log.info("禁用模型，id: {}", id);

        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));

        modelConfig.setEnabled(false);
        modelConfig.setUpdatedAt(LocalDateTime.now());
        ModelConfig updatedModel = modelConfigRepository.save(modelConfig);

        log.info("模型禁用成功，id: {}", id);
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
}
