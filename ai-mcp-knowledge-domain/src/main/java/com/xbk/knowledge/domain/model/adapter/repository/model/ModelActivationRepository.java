package com.xbk.knowledge.domain.model.adapter.repository.model;

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
     * 为什么：全局只有一份激活配置
     * 入参：无
     * 出参：激活配置
     */
    ModelActivation queryActivation();

    /**
     * 保存或更新激活配置
     * <p>
     * 为什么：激活配置可能已存在，需要覆盖更新
     * 入参：激活配置
     * 出参：保存后的激活配置
     */
    void saveOrUpdate(ModelActivation activation);
}
