package com.xbk.knowledge.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.infrastructure.dao.po.AgentPO;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Agent DAO（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IAgentDao extends BaseMapper<AgentPO> {

    int insertAgent(AgentPO agent);

    int updateAgentByCode(AgentPO agent);

    AgentPO findByCode(AgentCodeQuery query);

    long count(AgentPageQuery query);

    List<AgentPO> findPage(AgentPageQuery query);

    long countPublished();
}
