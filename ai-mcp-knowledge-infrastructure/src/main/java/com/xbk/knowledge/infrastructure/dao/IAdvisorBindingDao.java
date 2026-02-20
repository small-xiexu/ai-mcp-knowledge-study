package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.AdvisorBindingPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.advisor.model.entity.AdvisorBinding;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingQuery;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AdvisorBinding Mapper（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IAdvisorBindingDao extends BaseMapper<AdvisorBindingPO> {

    List<AdvisorBinding> listBindings(@Param("q") AdvisorBindingQuery query);

    List<AdvisorBindingView> listBindingViews(@Param("q") AdvisorBindingQuery query);

    int deleteByTarget(@Param("q") AdvisorBindingQuery query);

    int insertBinding(AdvisorBinding binding);
}

