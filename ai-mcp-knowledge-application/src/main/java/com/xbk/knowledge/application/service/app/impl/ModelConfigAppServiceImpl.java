package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.armory.factory.DefaultAiClientArmoryStrategyFactory;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelActivation;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolBindingRepository;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelActivationRepository;
import com.xbk.knowledge.domain.llm.service.IModelConfigService;
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
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class ModelConfigAppServiceImpl implements ModelConfigAppService {
    /**
     * 模型配置领域服务，用于模型配置核心业务逻辑。
     */
    private final IModelConfigService modelConfigService;

    /**
     * 模型激活仓储，用于读写全局激活模型配置。
     */
    private final ModelActivationRepository modelActivationRepository;

    /**
     * 工具绑定仓储，用于模型删除前清理绑定关系。
     */
    private final McpToolBindingRepository toolBindingRepository;

    /**
     * 模型提供器工厂，用于连通性测试与模型能力装配。
     */
    private final ModelProviderFactory modelProviderFactory;

    /**
     * Armory 策略工厂，用于缓存失效与客户端重建。
     */
    private final DefaultAiClientArmoryStrategyFactory armoryStrategyFactory;

    /**
     * 分页查询模型配置
     *
     * 统一分页入口，隔离应用层与领域层的查询协议
     * 
     * @param query 分页查询条件。
     * @return 模型配置分页结果。
     */
    @Override
    public PageResult<ModelConfig> queryModelConfigPage(ModelConfigPageQuery query) {
        return modelConfigService.queryModelConfigPage(query);
    }

    /**
     * 根据 ID 查询模型配置
     *
     * 通过应用层统一入口获取详情，便于后续扩展校验
     * 
     * @param query 主键查询条件。
     * @return 模型配置详情。
     */
    @Override
    public ModelConfig queryModelConfigById(IdQuery query) {
        return modelConfigService.queryModelConfigById(query);
    }

    /**
     * 创建模型配置
     *
     * 由应用层控制事务边界，保证创建一致性
     * 
     * @param modelConfig 模型配置。
     * @return 创建后的模型配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig createModelConfig(ModelConfig modelConfig) {
        ModelConfig created = modelConfigService.createModelConfig(modelConfig);
        if (created != null) {
            armoryStrategyFactory.evictModel(created.getId());
        }
        return created;
    }

    /**
     * 更新模型配置
     *
     * 由应用层控制事务边界，保证更新一致性
     * 
     * @param modelConfig 模型配置。
     * @return 更新后的模型配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig updateModelConfig(ModelConfig modelConfig) {
        ModelConfig updated = modelConfigService.updateModelConfig(modelConfig);
        if (updated != null) {
            armoryStrategyFactory.evictModel(updated.getId());
        }
        return updated;
    }

    /**
     * 删除模型配置
     *
     * 由应用层控制事务边界，保证删除一致性
     * 
     * @param query 主键查询条件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelConfig(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        Long modelId = query.getId();
        clearModelReferences(modelId);
        modelConfigService.deleteModelConfig(query);
        armoryStrategyFactory.evictModel(modelId);
    }

    /**
     * 启用模型
     *
     * 由应用层控制事务边界，保证启用一致性
     * 
     * @param query 主键查询条件。
     * @return 启用后的模型配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig enableModel(IdQuery query) {
        ModelConfig enabled = modelConfigService.enableModel(query);
        if (enabled != null) {
            armoryStrategyFactory.evictModel(enabled.getId());
        }
        return enabled;
    }

    /**
     * 禁用模型
     *
     * 由应用层控制事务边界，保证禁用一致性
     * 
     * @param query 主键查询条件。
     * @return 禁用后的模型配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig disableModel(IdQuery query) {
        ModelConfig disabled = modelConfigService.disableModel(query);
        if (disabled != null) {
            armoryStrategyFactory.evictModel(disabled.getId());
        }
        return disabled;
    }

    /**
     * 查询所有启用的模型
     *
     * 提供启用模型集合供调度或推荐使用
     * 
     * @param query 启用状态查询条件。
     * @return 模型配置列表。
     */
    @Override
    public List<ModelConfig> queryEnabledModels(EnabledQuery query) {
        return modelConfigService.queryEnabledModels(query);
    }

    /**
     * 获取当前激活的对话模型
     *
     * 激活模型由独立配置表维护，便于全局读取
     * 
     * @return 当前激活的对话模型配置。
     */
    @Override
    public ModelConfig getActiveChatModel() {
        ModelActivation activation = modelActivationRepository.queryActivation();
        if (activation == null || activation.getChatModelId() == null) {
            return null;
        }
        // 通过配置表反查当前激活模型
        IdQuery query = new IdQuery(activation.getChatModelId());
        return modelConfigService.queryModelConfigById(query);
    }

    /**
     * 获取当前激活的嵌入模型
     *
     * 激活模型由独立配置表维护，便于全局读取
     * 
     * @return 当前激活的嵌入模型配置。
     */
    @Override
    public ModelConfig getActiveEmbeddingModel() {
        ModelActivation activation = modelActivationRepository.queryActivation();
        if (activation == null || activation.getEmbeddingModelId() == null) {
            return null;
        }
        // 通过配置表反查当前激活模型
        IdQuery query = new IdQuery(activation.getEmbeddingModelId());
        return modelConfigService.queryModelConfigById(query);
    }

    /**
     * 激活对话模型
     *
     * 通过激活表保证同一时刻只有一个对话模型
     * 
     * @param query 主键查询条件。
     * @return 已激活的对话模型配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig activateChatModel(IdQuery query) {
        ModelConfig modelConfig = modelConfigService.queryModelConfigById(query);
        if (modelConfig == null) {
            return null;
        }
        // 保留当前嵌入模型配置，避免覆盖
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
     *
     * 通过激活表保证同一时刻只有一个嵌入模型
     * 
     * @param query 主键查询条件。
     * @return 已激活的嵌入模型配置。
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
        // 保留当前对话模型配置，避免覆盖
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
     *
     * 统一通过 Provider 检查健康度，避免不同调用方实现不一致
     * 
     * @param modelConfig 模型配置。
     * @return `true` 表示连接可用，`false` 表示连接不可用。
     */
    @Override
    public boolean testModelConnection(ModelConfig modelConfig) {
        if (modelConfig == null || modelConfig.getModelType() == null) {
            return false;
        }
        return modelProviderFactory.getProvider(modelConfig.getModelType())
                .isHealthy(modelConfig);
    }

    /**
     * 清理模型删除前的业务引用，避免残留脏数据。
     * 
     * @param modelId 模型 ID。
     */
    private void clearModelReferences(Long modelId) {
        ModelActivation activation = modelActivationRepository.queryActivation();
        if (activation != null) {
            boolean changed = false;
            if (modelId.equals(activation.getChatModelId())) {
                activation.setChatModelId(null);
                changed = true;
            }
            if (modelId.equals(activation.getEmbeddingModelId())) {
                activation.setEmbeddingModelId(null);
                changed = true;
            }
            if (changed) {
                modelActivationRepository.saveOrUpdate(activation);
            }
        }
        toolBindingRepository.deleteByBindTypeAndTargetId(new ToolBindingQuery("MODEL", modelId));
    }
}
