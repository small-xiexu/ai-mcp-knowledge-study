package com.xbk.knowledge.domain.agent.service.impl;

import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentScheduleRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentVersionRepository;
import com.xbk.knowledge.domain.advisor.adapter.repository.AdvisorBindingRepository;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingQuery;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.agent.service.IAgentService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 领域服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentScheduleRepository agentScheduleRepository;
    private final AgentRunContextRepository agentRunContextRepository;
    private final AgentRunRepository agentRunRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AdvisorBindingRepository advisorBindingRepository;

    /**
     * 查询智能体。
     *
     * @param query 查询条件
     * @return 返回 Agent 分页数据。
     */
    @Override
    public PageResult<Agent> queryPage(AgentPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query 不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        AgentPageQuery normalized = new AgentPageQuery(
                query.getKeyword(),
                query.getStatus(),
                offset,
                pageSize
        );
        List<Agent> records = agentRepository.findPage(normalized);
        long total = agentRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 查询智能体。
     *
     * @param query 查询条件
     * @return 返回 Agent 数据。
     */
    @Override
    public Agent queryByCode(AgentCodeQuery query) {
        if (query == null || query.getAgentCode() == null) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        return agentRepository
                .findByCode(query)
                .orElseThrow(() -> new NotFoundException("Agent 不存在，agentCode: " + query.getAgentCode()));
    }

    /**
     * 创建并持久化智能体数据。
     *
     * @param agent 智能体实体。
     * @return 智能体保存结果。
     */
    @Override
    public Agent create(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("agent 不能为空");
        }
        if (agent.getAgentCode() == null || agent.getAgentCode().isBlank()) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        if (agent.getAgentName() == null || agent.getAgentName().isBlank()) {
            throw new IllegalArgumentException("agentName 不能为空");
        }
        AgentCodeQuery codeQuery = new AgentCodeQuery(agent.getAgentCode());
        if (agentRepository.existsByCode(codeQuery)) {
            throw new BusinessException("agentCode 已存在：" + agent.getAgentCode());
        }
        LocalDateTime now = LocalDateTime.now();
        if (agent.getStatus() == null || agent.getStatus().isBlank()) {
            agent.setStatus("ENABLED");
        }
        if (agent.getChannel() == null || agent.getChannel().isBlank()) {
            agent.setChannel("agent");
        }
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);
        return agentRepository.insert(agent);
    }

    /**
     * 更新智能体数据。
     *
     * @param agent 智能体实体。
     * @return 智能体更新结果。
     */
    @Override
    public Agent update(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("agent 不能为空");
        }
        if (agent.getAgentCode() == null || agent.getAgentCode().isBlank()) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        Agent existed = queryByCode(new AgentCodeQuery(agent.getAgentCode()));
        existed.setAgentName(agent.getAgentName());
        existed.setDescription(agent.getDescription());
        if (agent.getChannel() != null && !agent.getChannel().isBlank()) {
            existed.setChannel(agent.getChannel());
        }
        if (agent.getStatus() != null && !agent.getStatus().isBlank()) {
            existed.setStatus(agent.getStatus());
        }
        if (agent.getCurrentPublishedVersionId() != null) {
            existed.setCurrentPublishedVersionId(agent.getCurrentPublishedVersionId());
        }
        existed.setUpdatedBy(agent.getUpdatedBy());
        existed.setUpdatedAt(LocalDateTime.now());
        int affected = agentRepository.updateByCode(existed);
        if (affected <= 0) {
            throw new BusinessException("Agent 更新失败，agentCode: " + agent.getAgentCode());
        }
        return queryByCode(new AgentCodeQuery(agent.getAgentCode()));
    }

    /**
     * 删除智能体数据。
     *
     * @param query 查询条件
     */
    @Override
    public void remove(AgentCodeQuery query) {
        Agent existed = queryByCode(query);
        Long agentId = existed.getId();
        if (agentId == null) {
            throw new BusinessException("Agent 缺少有效 ID，无法删除");
        }

        // 1) 删除调度配置
        agentScheduleRepository.deleteByAgentId(agentId);

        // 2) 删除版本级 Advisor 绑定
        List<AgentVersion> versions = agentVersionRepository.listByAgentId(agentId);
        if (versions != null && !versions.isEmpty()) {
            for (AgentVersion version : versions) {
                if (version == null || version.getId() == null) {
                    continue;
                }
                advisorBindingRepository.deleteByTarget(new AdvisorBindingQuery("AGENT_VERSION", version.getId()));
            }
        }

        // 3) 删除审批单 / 运行上下文 / 运行记录 / 版本
        approvalRequestRepository.deleteByAgentId(agentId);
        agentRunContextRepository.deleteByAgentId(agentId);
        agentRunRepository.deleteByAgentId(agentId);
        agentVersionRepository.removeByAgentId(agentId);

        // 4) 删除 Agent 主记录
        int affected = agentRepository.deleteByCode(query);
        if (affected <= 0) {
            throw new BusinessException("Agent 删除失败，agentCode: " + query.getAgentCode());
        }
    }
}
