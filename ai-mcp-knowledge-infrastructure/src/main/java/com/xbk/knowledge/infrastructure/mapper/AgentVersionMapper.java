package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AgentVersion Mapper（通过 XML 承载 SQL）。
 */
@Mapper
public interface AgentVersionMapper extends BaseMapper<AgentVersion> {

    int insertAgentVersion(AgentVersion version);

    int updateDraft(AgentVersion version);

    AgentVersion findById(AgentVersionIdQuery query);

    List<AgentVersion> findPage(AgentVersionPageQuery query);

    long count(AgentVersionPageQuery query);

    Integer findMaxVersionNo(@Param("agentId") Long agentId);

    int countByAgentIdAndVersionNo(@Param("agentId") Long agentId, @Param("versionNo") Integer versionNo);

    int publish(@Param("orgId") Long orgId,
                @Param("id") Long id,
                @Param("promptTemplateVersionNo") Integer promptTemplateVersionNo,
                @Param("templateParamsJson") String templateParamsJson,
                @Param("systemPromptSnapshot") String systemPromptSnapshot,
                @Param("updatedBy") Long updatedBy);

    int updateState(@Param("orgId") Long orgId,
                    @Param("id") Long id,
                    @Param("fromState") String fromState,
                    @Param("toState") String toState);
}
