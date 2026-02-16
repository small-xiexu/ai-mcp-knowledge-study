package com.xbk.knowledge.infrastructure.mapper.advisor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.advisor.Advisor;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Advisor Mapper（通过 XML 承载 SQL）。
 
  * @author xiexu
  */
@Mapper
public interface AdvisorMapper extends BaseMapper<Advisor> {

    Advisor findById(@Param("orgId") Long orgId, @Param("id") Long id);

    Advisor findByCode(@Param("orgId") Long orgId, @Param("advisorCode") String advisorCode);

    List<Advisor> findPage(@Param("q") AdvisorPageQuery query);

    long count(@Param("q") AdvisorPageQuery query);

    int insertAdvisor(Advisor advisor);

    int updateAdvisor(Advisor advisor);

    int updateEnabled(@Param("orgId") Long orgId, @Param("id") Long id, @Param("enabled") Integer enabled);

    int deleteById(@Param("orgId") Long orgId, @Param("id") Long id);
}

