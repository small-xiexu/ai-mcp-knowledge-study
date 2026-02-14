package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import com.xbk.knowledge.domain.model.vo.tool.ToolPolicyPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ToolPolicy Mapper（通过 XML 承载 SQL）。
 *
 * @author xiexu
 */
@Mapper
public interface ToolPolicyMapper extends BaseMapper<ToolPolicy> {

    ToolPolicy findEnabled(@Param("orgId") Long orgId, @Param("toolKey") String toolKey);

    ToolPolicy findById(@Param("orgId") Long orgId, @Param("id") Long id);

    ToolPolicy findByToolKey(@Param("orgId") Long orgId, @Param("toolKey") String toolKey);

    List<ToolPolicy> findPage(@Param("q") ToolPolicyPageQuery query);

    long count(@Param("q") ToolPolicyPageQuery query);

    int insertPolicy(ToolPolicy policy);

    int updatePolicy(ToolPolicy policy);

    int updateEnabled(@Param("orgId") Long orgId, @Param("id") Long id, @Param("enabled") Integer enabled);

    int deleteById(@Param("orgId") Long orgId, @Param("id") Long id);
}
