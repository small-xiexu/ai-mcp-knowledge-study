package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.SysApiKey;
import com.xbk.knowledge.domain.model.vo.identity.ApiKeyPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * API Key Mapper。
 *
 * 职责：MyBatis Mapper 接口，用于执行 API Key 相关 SQL。
 *
 * @author xiexu
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<SysApiKey> {

    /**
     * 分页查询 API Key。
     *
     * @param query 查询条件
     * @return 列表
     */
    List<SysApiKey> findPage(ApiKeyPageQuery query);

    /**
     * 统计 API Key 数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    long count(ApiKeyPageQuery query);

    /**
     * 插入 API Key。
     *
     * @param apiKey 实体
     * @return 影响行数
     */
    int insertApiKey(SysApiKey apiKey);

    /**
     * 按ID查询 API Key。
     *
     * @param id ID
     * @return API Key
     */
    SysApiKey findById(@Param("id") Long id);

    /**
     * 更新 API Key 状态。
     *
     * @param id ID
     * @param tenantId 租户ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id,
                     @Param("tenantId") String tenantId,
                     @Param("status") Integer status);
}
