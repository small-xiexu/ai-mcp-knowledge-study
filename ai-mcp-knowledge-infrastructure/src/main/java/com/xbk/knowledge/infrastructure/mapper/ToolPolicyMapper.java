package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * ToolPolicy Mapper（通过 XML 承载 SQL）。
 *
 * @author xiexu
 */
@Mapper
public interface ToolPolicyMapper extends BaseMapper<ToolPolicy> {

    ToolPolicy findEnabled(@Param("orgId") Long orgId, @Param("toolKey") String toolKey);
}

