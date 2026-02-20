package com.xbk.knowledge.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.infrastructure.dao.po.AgentVersionPO;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AgentVersion DAO（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IAgentVersionDao extends BaseMapper<AgentVersionPO> {

    int insertAgentVersion(AgentVersionPO version);

    int updateDraft(AgentVersionPO version);

    AgentVersionPO findById(AgentVersionIdQuery query);

    List<AgentVersionPO> findPage(AgentVersionPageQuery query);

    long count(AgentVersionPageQuery query);

    Integer findMaxVersionNo(@Param("agentId") Long agentId);

    int countByAgentIdAndVersionNo(@Param("agentId") Long agentId, @Param("versionNo") Integer versionNo);

    int publish(@Param("id") Long id,
                @Param("promptTemplateVersionNo") Integer promptTemplateVersionNo,
                @Param("templateParamsJson") String templateParamsJson,
                @Param("systemPromptSnapshot") String systemPromptSnapshot,
                @Param("updatedBy") Long updatedBy);

    int updateState(@Param("id") Long id,
                    @Param("fromState") String fromState,
                    @Param("toState") String toState);
}
