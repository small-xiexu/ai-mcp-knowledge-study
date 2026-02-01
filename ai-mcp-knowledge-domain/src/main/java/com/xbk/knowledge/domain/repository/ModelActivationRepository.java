package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ModelActivation;

/**
 * 模型激活配置仓储接口
 *
 * 职责：模型激活数据持久化访问
 * @author xiexu
 */
public interface ModelActivationRepository {

    /**
     * 查询当前激活配置
     *
     * @return 激活配置
     */
    ModelActivation queryActivation();

    /**
     * 保存或更新激活配置
     *
     * @param activation 激活配置
     * @return 保存后的激活配置
     */
    ModelActivation saveOrUpdate(ModelActivation activation);
}
