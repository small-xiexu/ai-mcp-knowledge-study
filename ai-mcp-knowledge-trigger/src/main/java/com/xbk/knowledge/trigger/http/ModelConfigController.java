package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IModelConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigQueryRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.common.ResultCode;
import com.xbk.knowledge.api.dto.model.ModelConfigRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigResponse;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.types.enums.ModelType;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
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
 * @author sxie
 */
@Slf4j
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelConfigController implements IModelConfigService {

    private final ModelConfigAppService modelConfigAppService;

    /**
     * 查询所有可用模型（分页）
     *
     * 为什么：模型数量可能增长，分页保证接口稳定
     * 入参：分页查询请求
     * 出参：分页结果（包含激活态标记）
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    @Override
    public Result<PageResult<ModelConfigResponse>> listModels(@Valid @RequestBody ModelConfigQueryRequest request) {
        /*
         * 目的：将分页参数转为领域查询对象，隔离接口层字段
 */
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        ModelConfigPageQuery query = new ModelConfigPageQuery(
                offset,
                pageSize
        );
        PageResult<ModelConfig> pageResult = modelConfigAppService.queryModelConfigPage(query);

        /*
         * 目的：补充激活状态，前端无需额外查询
 */
        ModelConfig activeChatModel = modelConfigAppService.getActiveChatModel();
        ModelConfig activeEmbeddingModel = modelConfigAppService.getActiveEmbeddingModel();
        Long activeChatId = activeChatModel != null ? activeChatModel.getId() : null;
        Long activeEmbeddingId = activeEmbeddingModel != null ? activeEmbeddingModel.getId() : null;

        /*
         * 目的：统一分页转换逻辑，确保响应结构与前端协议一致
 */
        PageResult<ModelConfigResponse> result = PageResultConverter.convert(
                pageResult,
                modelConfig -> convertToResponse(modelConfig, activeChatId, activeEmbeddingId)
        );

        return Result.success(result);
    }

    /**
     * 根据 ID 查询模型配置
     *
     * 为什么：前端详情页需要单条记录
     * 入参：ID 查询请求
     * 出参：模型配置详情
     */
    @PostMapping("/get")
    @SaCheckPermission("agent:read")
    @Override
    public Result<ModelConfigResponse> getModel(@Valid @RequestBody IdRequest request) {
        /*
         * 目的：查询模型配置并补充激活状态
 */
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig modelConfig = modelConfigAppService.queryModelConfigById(idQuery);
        ModelConfig activeChatModel = modelConfigAppService.getActiveChatModel();
        ModelConfig activeEmbeddingModel = modelConfigAppService.getActiveEmbeddingModel();
        Long activeChatId = activeChatModel != null ? activeChatModel.getId() : null;
        Long activeEmbeddingId = activeEmbeddingModel != null ? activeEmbeddingModel.getId() : null;

        /*
         * 目的：输出层只暴露必要字段
 */
        ModelConfigResponse response = convertToResponse(modelConfig, activeChatId, activeEmbeddingId);
        return Result.success(response);
    }

    /**
     * 创建模型配置
     *
     * 为什么：模型配置统一由应用层校验与持久化
     * 入参：模型配置请求
     * 出参：创建的模型配置
     */
    @PostMapping("/create")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ModelConfigResponse> createModel(@Valid @RequestBody ModelConfigRequest request) {
        /*
         * 目的：从接口请求构建领域实体，隔离 DTO 与领域模型
 */
        ModelConfig modelConfig = buildModelConfigFromRequest(request);

        /*
         * 目的：交由应用层完成持久化与业务校验
 */
        ModelConfig savedModel = modelConfigAppService.createModelConfig(modelConfig);

        /*
         * 目的：输出层只返回必要字段
 */
        ModelConfigResponse response = convertToResponse(savedModel, null, null);
        return Result.success("模型配置创建成功", response);
    }

    /**
     * 更新模型配置
     *
     * 为什么：保持配置变更入口统一，便于审计与回溯
     * 入参：模型配置请求（包含 ID）
     * 出参：更新后的模型配置
     */
    @PostMapping("/update")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ModelConfigResponse> updateModel(@Valid @RequestBody ModelConfigRequest request) {
        /*
         * 目的：构建完整领域实体，确保字段映射一致
 */
        ModelConfig modelConfig = buildModelConfigFromRequest(request);
        Long id = request.getId();
        modelConfig.setId(id);

        /*
         * 目的：交由应用层处理更新逻辑
 */
        ModelConfig updatedModel = modelConfigAppService.updateModelConfig(modelConfig);

        /*
         * 目的：输出层只返回必要字段
 */
        ModelConfigResponse response = convertToResponse(updatedModel, null, null);
        return Result.success("模型配置更新成功", response);
    }

    /**
     * 删除模型配置
     *
     * 为什么：清理无效配置，避免运行时引用错误
     * 入参：ID 查询请求
     * 出参：删除结果
     */
    @PostMapping("/delete")
    @SaCheckPermission("agent:write")
    @Override
    public Result<Void> deleteModel(@Valid @RequestBody IdRequest request) {
        /*
         * 目的：由应用层完成删除与校验
 */
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        modelConfigAppService.deleteModelConfig(idQuery);

        return Result.success();
    }

    /**
     * 启用模型
     *
     * 为什么：允许模型进入可用列表，供后续激活或选择
     * 入参：ID 查询请求
     * 出参：操作结果
     */
    @PostMapping("/enable")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ModelConfigResponse> enableModel(@Valid @RequestBody IdRequest request) {
        /*
         * 目的：交由应用层处理启用逻辑与校验
 */
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig updatedModel = modelConfigAppService.enableModel(idQuery);

        /*
         * 目的：输出层只返回必要字段
 */
        ModelConfigResponse response = convertToResponse(updatedModel, null, null);
        return Result.success("模型启用成功", response);
    }

    /**
     * 禁用模型
     *
     * 为什么：禁用不可用模型，防止被选择或误用
     * 入参：ID 查询请求
     * 出参：操作结果
     */
    @PostMapping("/disable")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ModelConfigResponse> disableModel(@Valid @RequestBody IdRequest request) {
        /*
         * 目的：交由应用层处理禁用逻辑与校验
 */
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig updatedModel = modelConfigAppService.disableModel(idQuery);

        /*
         * 目的：输出层只返回必要字段
 */
        ModelConfigResponse response = convertToResponse(updatedModel, null, null);
        return Result.success("模型禁用成功", response);
    }

    /**
     * 获取当前激活的对话模型
     *
     * 为什么：前端需要展示当前对话模型，便于配置确认
     * 入参：无
     * 出参：当前激活的对话模型
     */
    @PostMapping("/active-chat")
    @SaCheckPermission("agent:read")
    @Override
    public Result<ModelConfigResponse> getActiveChatModel() {
        ModelConfig modelConfig = modelConfigAppService.getActiveChatModel();
        if (modelConfig == null) {
            return Result.success(null);
        }
        ModelConfigResponse response = convertToResponse(modelConfig, modelConfig.getId(), null);
        return Result.success(response);
    }

    /**
     * 获取当前激活的嵌入模型
     *
     * 为什么：前端需要展示当前嵌入模型，便于配置确认
     * 入参：无
     * 出参：当前激活的嵌入模型
     */
    @PostMapping("/active-embedding")
    @SaCheckPermission("agent:read")
    @Override
    public Result<ModelConfigResponse> getActiveEmbeddingModel() {
        ModelConfig modelConfig = modelConfigAppService.getActiveEmbeddingModel();
        if (modelConfig == null) {
            return Result.success(null);
        }
        ModelConfigResponse response = convertToResponse(modelConfig, null, modelConfig.getId());
        return Result.success(response);
    }

    /**
     * 激活对话模型
     *
     * 为什么：确保对话模型唯一激活，交由应用层做互斥处理
     * 入参：ID 查询请求
     * 出参：激活后的模型配置
     */
    @PostMapping("/activate-chat")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ModelConfigResponse> activateChatModel(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig modelConfig = modelConfigAppService.activateChatModel(idQuery);
        if (modelConfig == null) {
            return Result.error("未找到模型配置");
        }
        ModelConfigResponse response = convertToResponse(modelConfig, modelConfig.getId(), null);
        return Result.success("对话模型激活成功", response);
    }

    /**
     * 激活嵌入模型
     *
     * 为什么：确保嵌入模型唯一激活，交由应用层做互斥处理
     * 入参：ID 查询请求
     * 出参：激活后的模型配置
     */
    @PostMapping("/activate-embedding")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ModelConfigResponse> activateEmbeddingModel(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig modelConfig = modelConfigAppService.activateEmbeddingModel(idQuery);
        if (modelConfig == null) {
            return Result.error("未找到模型配置");
        }
        ModelConfigResponse response = convertToResponse(modelConfig, null, modelConfig.getId());
        return Result.success("嵌入模型激活成功", response);
    }

    /**
     * 测试模型配置连接
     *
     * 为什么：在保存前校验连通性，避免无效配置进入生产
     * 入参：ID 查询请求
     * 出参：测试结果
     */
    @PostMapping("/test")
    @SaCheckPermission("agent:write")
    @Override
    public Result<Boolean> testModel(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        ModelConfig modelConfig = modelConfigAppService.queryModelConfigById(idQuery);
        if (modelConfig == null) {
            return Result.error("未找到模型配置");
        }
        boolean result = modelConfigAppService.testModelConnection(modelConfig);
        if (result) {
            return Result.success("模型连接成功", true);
        }
        return Result.error(ResultCode.INTERNAL_ERROR, "模型连接失败", false);
    }

    /**
     * 转换为响应 DTO
     *
     * 为什么：输出层只暴露必要字段，并补充激活态标记
     * 入参：模型配置实体、激活模型 ID
     * 出参：响应 DTO
     */
    private ModelConfigResponse convertToResponse(ModelConfig modelConfig, Long activeChatId, Long activeEmbeddingId) {
        Long modelId = modelConfig.getId();
        String modelName = modelConfig.getModelName();
        ModelType modelType = modelConfig.getModelType();
        String baseUrl = modelConfig.getBaseUrl();
        String completionsPath = modelConfig.getCompletionsPath();
        String embeddingsPath = modelConfig.getEmbeddingsPath();
        String apiKey = modelConfig.getApiKey();
        Boolean enabled = modelConfig.getEnabled();
        Boolean toolEnabled = modelConfig.getToolEnabled();
        LocalDateTime createdAt = modelConfig.getCreatedAt();
        LocalDateTime updatedAt = modelConfig.getUpdatedAt();
        Boolean activeChat = activeChatId != null && activeChatId.equals(modelId);
        Boolean activeEmbedding = activeEmbeddingId != null && activeEmbeddingId.equals(modelId);
        return ModelConfigResponse.builder()
                .id(modelId)
                .modelName(modelName)
                .modelType(modelType)
                .baseUrl(baseUrl)
                .completionsPath(completionsPath)
                .embeddingsPath(embeddingsPath)
                .apiKey(apiKey)
                .enabled(enabled)
                .toolEnabled(toolEnabled)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .activeChat(activeChat)
                .activeEmbedding(activeEmbedding)
                .build();
    }

    /**
     * 从请求 DTO 构建领域实体
     *
     * 为什么：保证领域对象构建过程集中，便于统一校验与扩展
     * 入参：请求 DTO
     * 出参：领域实体
     */
    private ModelConfig buildModelConfigFromRequest(ModelConfigRequest request) {
        String modelName = request.getModelName();
        ModelType modelType = request.getModelType();
        String apiKey = request.getApiKey();
        String baseUrl = request.getBaseUrl();
        String completionsPath = request.getCompletionsPath();
        String embeddingsPath = request.getEmbeddingsPath();
        Boolean enabled = request.getEnabled();
        Boolean toolEnabled = request.getToolEnabled();
        if (toolEnabled == null) {
            toolEnabled = true;
        }
        ModelConfig modelConfig = ModelConfig.builder()
                .modelName(modelName)
                .modelType(modelType)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .completionsPath(completionsPath)
                .embeddingsPath(embeddingsPath)
                .enabled(enabled)
                .toolEnabled(toolEnabled)
                .build();

        return modelConfig;
    }
}
