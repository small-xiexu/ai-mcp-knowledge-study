package com.xbk.knowledge.infrastructure.mapper.advisor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.advisor.AdvisorBinding;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorBindingQuery;
import com.xbk.knowledge.domain.model.vo.advisor.AdvisorBindingView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AdvisorBinding Mapper（通过 XML 承载 SQL）。
 
  * @author xiexu
  */
@Mapper
public interface AdvisorBindingMapper extends BaseMapper<AdvisorBinding> {

    List<AdvisorBinding> listBindings(@Param("q") AdvisorBindingQuery query);

    List<AdvisorBindingView> listBindingViews(@Param("q") AdvisorBindingQuery query);

    int deleteByTarget(@Param("q") AdvisorBindingQuery query);

    int insertBinding(AdvisorBinding binding);
}

