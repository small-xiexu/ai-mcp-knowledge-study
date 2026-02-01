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
     * 为什么：全局仅维护一份激活配置
     * 入参：无
     * 出参：激活配置
     */
    ModelActivation findActivation();

    /**
     * 插入激活配置
     *
     * 为什么：首次创建激活配置
     * 入参：激活配置
     * 出参：无
     */
    void insertActivation(@Param("activation") ModelActivation activation);

    /**
     * 更新激活配置
     *
     * 为什么：更新当前激活模型
     * 入参：激活配置
     * 出参：无
     */
    void updateActivation(@Param("activation") ModelActivation activation);
}
