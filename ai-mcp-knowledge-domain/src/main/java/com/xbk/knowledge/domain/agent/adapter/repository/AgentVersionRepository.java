package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * AgentVersion 仓储接口。
 *
 * @author sxie
 */
public interface AgentVersionRepository {

    /**
     * 按主键查询记录。
     */
    Optional<AgentVersion> findById(AgentVersionIdQuery query);

    /**
     * 按条件分页查询记录。
     */
    List<AgentVersion> findPage(AgentVersionPageQuery query);

    /**
     * 查询 Agent 关联记录列表。
     */
    List<AgentVersion> listByAgentId(Long agentId);

    /**
     * 统计符合条件的记录数量。
     */
    long count(AgentVersionPageQuery query);

    /**
     * 查询 Agent 当前最大版本号。
     */
    Integer findMaxVersionNo(Long agentId);

    /**
     * 判断 Agent 下版本号是否已存在。
     */
    boolean existsByAgentIdAndVersionNo(Long agentId, Integer versionNo);

    /**
     * 新增记录。
     */
    AgentVersion insert(AgentVersion version);

    /**
     * 更新草稿版本内容。
     */
    int updateDraft(AgentVersion version);

    /**
     * 发布版本并写入快照信息。
     */
    int publish(Long id,
                Integer promptTemplateVersionNo,
                String templateParamsJson,
                String systemPromptSnapshot,
                Long updatedBy);

    /**
     * 按状态机规则更新状态。
     */
    int updateState(Long id, String fromState, String toState);

    /**
     * 删除 Agent 下全部版本记录。
     */
    int removeByAgentId(Long agentId);
}
