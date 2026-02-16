package com.xbk.knowledge.domain.service.agent.impl;

import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import com.xbk.knowledge.domain.repository.agent.AgentRepository;
import com.xbk.knowledge.domain.service.agent.IAgentService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 领域服务实现。
 
  * @author xiexu
  */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final AgentRepository agentRepository;

    /**
     * queryPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public PageResult<Agent> queryPage(AgentPageQuery query) {
        if (query == null || query.getOrgId() == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        AgentPageQuery normalized = new AgentPageQuery(
                query.getOrgId(),
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
     * queryByCode。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Agent queryByCode(AgentCodeQuery query) {
        if (query == null || query.getOrgId() == null || query.getAgentCode() == null) {
            throw new IllegalArgumentException("orgId/agentCode 不能为空");
        }
        return agentRepository
                .findByCode(query)
                .orElseThrow(() -> new NotFoundException("Agent 不存在，agentCode: " + query.getAgentCode()));
    }

    /**
     * create。
     *
     * @param agent 参数
     * @return 返回结果
     */
    @Override
    public Agent create(Agent agent) {
        if (agent == null || agent.getOrgId() == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        if (agent.getAgentCode() == null || agent.getAgentCode().isBlank()) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        if (agent.getAgentName() == null || agent.getAgentName().isBlank()) {
            throw new IllegalArgumentException("agentName 不能为空");
        }
        AgentCodeQuery codeQuery = new AgentCodeQuery(agent.getOrgId(), agent.getAgentCode());
        if (agentRepository.existsByCode(codeQuery)) {
            throw new BusinessException("agentCode 已存在：" + agent.getAgentCode());
        }
        LocalDateTime now = LocalDateTime.now();
        if (agent.getStatus() == null || agent.getStatus().isBlank()) {
            agent.setStatus("ENABLED");
        }
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);
        return agentRepository.insert(agent);
    }

    /**
     * update。
     *
     * @param agent 参数
     * @return 返回结果
     */
    @Override
    public Agent update(Agent agent) {
        if (agent == null || agent.getOrgId() == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        if (agent.getAgentCode() == null || agent.getAgentCode().isBlank()) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        Agent existed = queryByCode(new AgentCodeQuery(agent.getOrgId(), agent.getAgentCode()));
        existed.setAgentName(agent.getAgentName());
        existed.setDescription(agent.getDescription());
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
        return queryByCode(new AgentCodeQuery(agent.getOrgId(), agent.getAgentCode()));
    }
}

