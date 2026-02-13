package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.SysOrg;
import com.xbk.knowledge.domain.model.vo.identity.OrgQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组织 Mapper。
 *
 * 职责：MyBatis Mapper 接口，用于执行组织相关 SQL。
 *
 * @author xiexu
 */
@Mapper
public interface OrgMapper extends BaseMapper<SysOrg> {

    /**
     * 查询组织列表。
     *
     * @param query 查询条件
     * @return 列表
     */
    List<SysOrg> findList(OrgQuery query);

    /**
     * 按ID查询组织。
     *
     * @param orgId 组织ID
     * @return 组织
     */
    SysOrg findById(@Param("orgId") Long orgId);

    /**
     * 插入组织。
     *
     * @param org 组织实体
     * @return 影响行数
     */
    int insertOrg(SysOrg org);

    /**
     * 更新组织。
     *
     * @param org 组织实体
     * @return 影响行数
     */
    int updateOrg(SysOrg org);

    /**
     * 统计组织编码数量。
     *
     * @param orgCode 组织编码
     * @param excludeId 排除ID
     * @return 数量
     */
    long countByOrgCode(@Param("orgCode") String orgCode,
                        @Param("excludeId") Long excludeId);

    /**
     * 删除用户组织绑定。
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteUserOrgs(@Param("userId") Long userId);

    /**
     * 新增用户组织绑定。
     *
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param isPrimary 是否主组织
     * @return 影响行数
     */
    int insertUserOrg(@Param("userId") Long userId,
                      @Param("orgId") Long orgId,
                      @Param("isPrimary") Integer isPrimary);
}
