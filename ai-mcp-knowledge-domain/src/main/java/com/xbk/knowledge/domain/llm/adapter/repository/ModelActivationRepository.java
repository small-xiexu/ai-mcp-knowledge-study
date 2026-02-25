package com.xbk.knowledge.domain.llm.adapter.repository;

import com.xbk.knowledge.domain.llm.model.entity.ModelActivation;

/**
 * 模型激活配置仓储接口
 *
 * 职责：模型激活数据持久化访问
 * @author sxie
 */
public interface ModelActivationRepository {

    /**
     * 查询当前激活配置
     *
     * 全局只有一份激活配置
     * 
     * @return 当前激活配置。
     */
    ModelActivation queryActivation();

    /**
     * 保存或更新激活配置
     * <p>
     * 激活配置可能已存在，需要覆盖更新
     * 
     * @param activation 待保存的激活配置。
     */
    void saveOrUpdate(ModelActivation activation);
}
