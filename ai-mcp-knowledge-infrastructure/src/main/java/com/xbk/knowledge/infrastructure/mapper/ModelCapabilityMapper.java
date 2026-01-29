package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 模型能力 Mapper
 * 使用 XML 承载所有 SQL
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 * @author xiexu
 */
@Mapper
public interface ModelCapabilityMapper extends BaseMapper<ModelCapability> {

    /**
     * 新增模型能力
     *
     * @param capability 模型能力
     * @return 影响行数
     */
    int insertModelCapability(ModelCapability capability);

    /**
     * 更新模型能力
     *
     * @param capability 模型能力
     * @return 影响行数
     */
    int updateModelCapability(ModelCapability capability);

    /**
     * 根据模型ID查询模型能力
     *
     * @param modelId 模型ID
     * @return 模型能力
     */
    ModelCapability findByModelId(@Param("modelId") Long modelId);

    /**
     * 根据模型ID删除模型能力
     *
     * @param modelId 模型ID
     * @return 影响行数
     */
    int deleteByModelId(@Param("modelId") Long modelId);
}
