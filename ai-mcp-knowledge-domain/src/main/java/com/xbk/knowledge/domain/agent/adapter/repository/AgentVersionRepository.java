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
     * 
     * @param query 主键查询条件。
     * @return 可选的智能体版本实体。
     */
    Optional<AgentVersion> findById(AgentVersionIdQuery query);

    /**
     * 按条件分页查询记录。
     * 
     * @param query 分页查询条件。
     * @return 智能体版本分页列表。
     */
    List<AgentVersion> findPage(AgentVersionPageQuery query);

    /**
     * 查询 Agent 关联记录列表。
     * 
     * @param agentId 智能体 ID。
     * @return 智能体版本列表。
     */
    List<AgentVersion> listByAgentId(Long agentId);

    /**
     * 统计符合条件的记录数量。
     * 
     * @param query 分页查询条件。
     * @return 统计数量。
     */
    long count(AgentVersionPageQuery query);

    /**
     * 查询 Agent 当前最大版本号。
     * 
     * @param agentId 智能体 ID。
     * @return 统计数量。
     */
    Integer findMaxVersionNo(Long agentId);

    /**
     * 判断 Agent 下版本号是否已存在。
     * 
     * @param agentId 智能体 ID。
     * @param versionNo 版本号。
     * @return `true` 表示版本号已存在，`false` 表示不存在。
     */
    boolean existsByAgentIdAndVersionNo(Long agentId, Integer versionNo);

    /**
     * 新增记录。
     * 
     * @param version 待新增的智能体版本实体。
     * @return 已持久化的智能体版本实体。
     */
    AgentVersion insert(AgentVersion version);

    /**
     * 更新草稿版本内容。
     * 
     * @param version 待更新的智能体版本实体。
     * @return 影响行数。
     */
    int updateDraft(AgentVersion version);

    /**
     * 发布版本并写入快照信息。
     * 
     * @param id 主键 ID。
     * @param promptTemplateVersionNo 提示词模板版本号。
     * @param templateParamsJson 工具参数 JSON。
     * @param systemPromptSnapshot 系统提示词快照。
     * @param updatedBy 操作人标识。
     * @return 影响行数。
     */
    int publish(Long id,
                Integer promptTemplateVersionNo,
                String templateParamsJson,
                String systemPromptSnapshot,
                Long updatedBy);

    /**
     * 按状态机规则更新状态。
     * 
     * @param id 主键 ID。
     * @param fromState 起始状态。
     * @param toState 目标状态。
     * @return 影响行数。
     */
    int updateState(Long id, String fromState, String toState);

    /**
     * 删除 Agent 下全部版本记录。
     * 
     * @param agentId 智能体 ID。
     * @return 影响行数。
     */
    int removeByAgentId(Long agentId);
}
