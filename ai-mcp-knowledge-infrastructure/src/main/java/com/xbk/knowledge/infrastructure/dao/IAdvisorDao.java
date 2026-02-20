package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.AdvisorPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.advisor.model.entity.Advisor;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Advisor Mapper（通过 XML 承载 SQL）。
 
  * @author xiexu
  */
@Mapper
public interface IAdvisorDao extends BaseMapper<AdvisorPO> {

    Advisor findById(@Param("id") Long id);

    Advisor findByCode(@Param("advisorCode") String advisorCode);

    List<Advisor> findPage(@Param("q") AdvisorPageQuery query);

    long count(@Param("q") AdvisorPageQuery query);

    int insertAdvisor(Advisor advisor);

    int updateAdvisor(Advisor advisor);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    int deleteById(@Param("id") Long id);
}

