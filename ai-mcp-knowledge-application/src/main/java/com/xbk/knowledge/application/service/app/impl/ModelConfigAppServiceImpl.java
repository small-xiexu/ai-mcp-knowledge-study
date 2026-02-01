package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.model.entity.ModelActivation;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeQuery;
import com.xbk.knowledge.domain.repository.ModelActivationRepository;
import com.xbk.knowledge.domain.service.IModelConfigService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型配置应用服务实现
 * 负责模型配置相关用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class ModelConfigAppServiceImpl implements ModelConfigAppService {

    private final IModelConfigService modelConfigService;
    private final ModelActivationRepository modelActivationRepository;
    private final ModelProviderFactory modelProviderFactory;

    /**
     * 分页查询模型配置
     * 负责应用层用例编排，调用领域服务获取分页结果
     */
    @Override
    public PageResult<ModelConfig> queryModelConfigPage(ModelConfigPageQuery query) {
        return modelConfigService.queryModelConfigPage(query);
    }

    /**
     * 根据 ID 查询模型配置
     * 负责应用层用例编排，调用领域服务获取模型详情
     */
    @Override
    public ModelConfig queryModelConfigById(IdQuery query) {
        return modelConfigService.queryModelConfigById(query);
    }

    /**
     * 创建模型配置
     * 负责应用层事务边界编排，确保创建操作一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig createModelConfig(ModelConfig modelConfig) {
        return modelConfigService.createModelConfig(modelConfig);
    }

    /**
     * 更新模型配置
     * 负责应用层事务边界编排，确保更新操作一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig updateModelConfig(ModelConfig modelConfig) {
        return modelConfigService.updateModelConfig(modelConfig);
    }

    /**
     * 删除模型配置
     * 负责应用层事务边界编排，确保删除操作一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelConfig(IdQuery query) {
        modelConfigService.deleteModelConfig(query);
    }

    /**
     * 启用模型
     * 负责应用层事务边界编排，确保启用操作一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig enableModel(IdQuery query) {
        return modelConfigService.enableModel(query);
    }

    /**
     * 禁用模型
     * 负责应用层事务边界编排，确保禁用操作一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig disableModel(IdQuery query) {
        return modelConfigService.disableModel(query);
    }

    /**
     * 查询所有启用的模型
     * 负责应用层用例编排，调用领域服务返回启用模型列表
     */
    @Override
    public List<ModelConfig> queryEnabledModels(EnabledQuery query) {
        return modelConfigService.queryEnabledModels(query);
    }

    /**
     * 获取推荐模型
     * 负责应用层用例编排，调用领域服务返回推荐模型
     */
    @Override
    public ModelConfig getRecommendedModel(TaskTypeQuery query) {
        return modelConfigService.getRecommendedModel(query);
    }

    /**
     * 获取当前激活的对话模型
     * 使用激活配置表记录当前对话模型
     */
    @Override
    public ModelConfig getActiveChatModel() {
        ModelActivation activation = modelActivationRepository.queryActivation();
        if (activation == null || activation.getChatModelId() == null) {
            return null;
        }
        IdQuery query = new IdQuery(activation.getChatModelId());
        return modelConfigService.queryModelConfigById(query);
    }

    /**
     * 获取当前激活的嵌入模型
     * 使用激活配置表记录当前嵌入模型
     */
    @Override
    public ModelConfig getActiveEmbeddingModel() {
        ModelActivation activation = modelActivationRepository.queryActivation();
        if (activation == null || activation.getEmbeddingModelId() == null) {
            return null;
        }
        IdQuery query = new IdQuery(activation.getEmbeddingModelId());
        return modelConfigService.queryModelConfigById(query);
    }

    /**
     * 激活对话模型
     * 通过更新激活表保证同一时刻只有一个对话模型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig activateChatModel(IdQuery query) {
        ModelConfig modelConfig = modelConfigService.queryModelConfigById(query);
        if (modelConfig == null) {
            return null;
        }
        ModelActivation existing = modelActivationRepository.queryActivation();
        Long embeddingModelId = existing != null ? existing.getEmbeddingModelId() : null;
        ModelActivation activation = ModelActivation.builder()
                .chatModelId(modelConfig.getId())
                .embeddingModelId(embeddingModelId)
                .build();
        modelActivationRepository.saveOrUpdate(activation);
        return modelConfig;
    }

    /**
     * 激活嵌入模型
     * 通过更新激活表保证同一时刻只有一个嵌入模型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig activateEmbeddingModel(IdQuery query) {
        ModelConfig modelConfig = modelConfigService.queryModelConfigById(query);
        if (modelConfig == null) {
            return null;
        }
        ModelType modelType = modelConfig.getModelType();
        if (modelType != ModelType.OPENAI && modelType != ModelType.OLLAMA) {
            throw new IllegalArgumentException("当前模型类型不支持作为嵌入模型");
        }
        ModelActivation existing = modelActivationRepository.queryActivation();
        Long chatModelId = existing != null ? existing.getChatModelId() : null;
        ModelActivation activation = ModelActivation.builder()
                .chatModelId(chatModelId)
                .embeddingModelId(modelConfig.getId())
                .build();
        modelActivationRepository.saveOrUpdate(activation);
        return modelConfig;
    }

    /**
     * 测试模型配置连接
     * 通过 Provider 健康检查验证连接可用性
     */
    @Override
    public boolean testModelConnection(ModelConfig modelConfig) {
        if (modelConfig == null || modelConfig.getModelType() == null) {
            return false;
        }
        return modelProviderFactory.getProvider(modelConfig.getModelType())
                .isHealthy(modelConfig);
    }
}
