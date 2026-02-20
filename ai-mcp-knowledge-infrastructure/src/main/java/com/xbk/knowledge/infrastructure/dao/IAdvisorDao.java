package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.AdvisorPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AdvisorPO Mapper（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IAdvisorDao extends BaseMapper<AdvisorPO> {

    AdvisorPO findById(@Param("id") Long id);

    AdvisorPO findByCode(@Param("advisorCode") String advisorCode);

    List<AdvisorPO> findPage(@Param("q") AdvisorPageQuery query);

    long count(@Param("q") AdvisorPageQuery query);

    int insertAdvisor(AdvisorPO advisor);

    int updateAdvisor(AdvisorPO advisor);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    int deleteById(@Param("id") Long id);
}

