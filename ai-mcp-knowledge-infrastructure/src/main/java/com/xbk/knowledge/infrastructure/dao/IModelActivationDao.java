package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.ModelActivationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 模型激活配置 Mapper
 *
 * 职责：数据访问适配层
 * @author sxie
 */
@Mapper
public interface IModelActivationDao {

    /**
     * 查询当前激活配置
     *
     * 为什么：全局仅维护一份激活配置
     * 入参：无
     * 出参：激活配置
     */
    ModelActivationPO findActivation();

    /**
     * 插入激活配置
     *
     * 为什么：首次创建激活配置
     * 入参：激活配置
     * 出参：无
     */
    void insertActivation(@Param("activation") ModelActivationPO activation);

    /**
     * 更新激活配置
     *
     * 为什么：更新当前激活模型
     * 入参：激活配置
     * 出参：无
     */
    void updateActivation(@Param("activation") ModelActivationPO activation);
}
