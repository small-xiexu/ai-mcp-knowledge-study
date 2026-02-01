package com.xbk.knowledge.infrastructure.mapper;

import com.xbk.knowledge.domain.model.entity.ModelActivation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 模型激活配置 Mapper
 *
 * 职责：数据访问适配层
 * @author xiexu
 */
@Mapper
public interface ModelActivationMapper {

    /**
     * 查询当前激活配置
     *
     * @return 激活配置
     */
    ModelActivation findActivation();

    /**
     * 插入激活配置
     *
     * @param activation 激活配置
     */
    void insertActivation(@Param("activation") ModelActivation activation);

    /**
     * 更新激活配置
     *
     * @param activation 激活配置
     */
    void updateActivation(@Param("activation") ModelActivation activation);
}
