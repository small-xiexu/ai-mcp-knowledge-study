package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeQuery;
import com.xbk.knowledge.domain.service.IModelConfigService;
import com.xbk.knowledge.types.common.PageResult;
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
}
