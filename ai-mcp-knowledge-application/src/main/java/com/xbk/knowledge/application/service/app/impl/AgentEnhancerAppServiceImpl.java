package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AgentEnhancerAppService;
import com.xbk.knowledge.application.service.runtime.AgentEnhancerRuntimeService;
import com.xbk.knowledge.domain.agentenhancer.model.entity.AgentEnhancer;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerPageQuery;
import com.xbk.knowledge.domain.agentenhancer.adapter.repository.AgentEnhancerBindingRepository;
import com.xbk.knowledge.domain.agentenhancer.adapter.repository.AgentEnhancerRepository;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * AgentEnhancer 控制面应用服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class AgentEnhancerAppServiceImpl implements AgentEnhancerAppService {

    private final AgentEnhancerRepository agentEnhancerRepository;
    private final AgentEnhancerBindingRepository agentEnhancerBindingRepository;
    private final AgentEnhancerRuntimeService agentEnhancerRuntimeService;

    /**
     * 查询 Agent 增强器（AgentEnhancer）配置。
     *
     * @param query 查询条件
     * @return 返回 AgentEnhancer 分页数据。
     */
    @Override
    public PageResult<AgentEnhancer> queryPage(AgentEnhancerPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query 不能为空");
        }
        int offset = query.offset() == null ? 0 : Math.max(query.offset(), 0);
        int pageSize = query.pageSize() == null ? 20 : Math.min(Math.max(query.pageSize(), 1), 200);
        AgentEnhancerPageQuery normalized = new AgentEnhancerPageQuery(
                StringUtils.hasText(query.keyword()) ? query.keyword().trim() : null,
                query.enabled(),
                StringUtils.hasText(query.agentEnhancerType()) ? query.agentEnhancerType().trim().toUpperCase(Locale.ROOT) : null,
                offset,
                pageSize
        );
        List<AgentEnhancer> records = agentEnhancerRepository.findPage(normalized);
        long total = agentEnhancerRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 查询 Agent 增强器（AgentEnhancer）配置。
     *
     * @param id 主键 ID
     * @return 返回 AgentEnhancer 数据。
     */
    @Override
    public AgentEnhancer get(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        return agentEnhancerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("AgentEnhancer 不存在，id=" + id));
    }

    /**
     * 创建或更新 Agent 增强器（AgentEnhancer）数据。
     *
     * @param agentEnhancer Agent 增强器（AgentEnhancer）实体。
     * @return 返回 Agent 增强器（AgentEnhancer）保存结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentEnhancer save(AgentEnhancer agentEnhancer) {
        if (agentEnhancer == null) {
            throw new IllegalArgumentException("agentEnhancer 不能为空");
        }
        if (!StringUtils.hasText(agentEnhancer.getAgentEnhancerCode())) {
            throw new BusinessException("agentEnhancerCode 不能为空");
        }
        if (!StringUtils.hasText(agentEnhancer.getAgentEnhancerName())) {
            throw new BusinessException("agentEnhancerName 不能为空");
        }
        if (!StringUtils.hasText(agentEnhancer.getAgentEnhancerType())) {
            throw new BusinessException("agentEnhancerType 不能为空");
        }
        normalize(agentEnhancer);

        if (agentEnhancer.getId() != null) {
            AgentEnhancer existed = get(agentEnhancer.getId());
            existed.setAgentEnhancerCode(agentEnhancer.getAgentEnhancerCode());
            existed.setAgentEnhancerName(agentEnhancer.getAgentEnhancerName());
            existed.setAgentEnhancerType(agentEnhancer.getAgentEnhancerType());
            existed.setEnabled(agentEnhancer.getEnabled());
            existed.setConfigJson(agentEnhancer.getConfigJson());
            int affected = agentEnhancerRepository.update(existed);
            if (affected <= 0) {
                throw new BusinessException("更新失败，id=" + agentEnhancer.getId());
            }
            agentEnhancerRuntimeService.evictAll();
            return get(existed.getId());
        }

        AgentEnhancer existed = agentEnhancerRepository.findByCode(agentEnhancer.getAgentEnhancerCode()).orElse(null);
        if (existed == null || existed.getId() == null) {
            agentEnhancerRepository.insert(agentEnhancer);
            agentEnhancerRuntimeService.evictAll();
            return get(agentEnhancer.getId());
        }
        existed.setAgentEnhancerName(agentEnhancer.getAgentEnhancerName());
        existed.setAgentEnhancerType(agentEnhancer.getAgentEnhancerType());
        existed.setEnabled(agentEnhancer.getEnabled());
        existed.setConfigJson(agentEnhancer.getConfigJson());
        int affected = agentEnhancerRepository.update(existed);
        if (affected <= 0) {
            throw new BusinessException("保存失败，agentEnhancerCode=" + agentEnhancer.getAgentEnhancerCode());
        }
        agentEnhancerRuntimeService.evictAll();
        return get(existed.getId());
    }

    /**
     * 启用业务配置。
     *
     * @param id 主键 ID
     * @return 返回 AgentEnhancer 数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentEnhancer enable(Long id) {
        AgentEnhancer existed = get(id);
        int affected = agentEnhancerRepository.updateEnabled(id, 1);
        if (affected <= 0) {
            throw new BusinessException("启用失败，id=" + id);
        }
        agentEnhancerRuntimeService.evictAll();
        return get(existed.getId());
    }

    /**
     * 禁用业务配置。
     *
     * @param id 主键 ID
     * @return 返回 AgentEnhancer 数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentEnhancer disable(Long id) {
        AgentEnhancer existed = get(id);
        int affected = agentEnhancerRepository.updateEnabled(id, 0);
        if (affected <= 0) {
            throw new BusinessException("禁用失败，id=" + id);
        }
        agentEnhancerRuntimeService.evictAll();
        return get(existed.getId());
    }

    /**
     * 删除 Agent 增强器（AgentEnhancer）数据。
     *
     * @param id 主键 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        AgentEnhancer existed = get(id);
        agentEnhancerBindingRepository.deleteByAgentEnhancerId(existed.getId());
        int affected = agentEnhancerRepository.deleteById(id);
        if (affected <= 0) {
            throw new BusinessException("删除失败，id=" + id);
        }
        agentEnhancerRuntimeService.evictAll();
    }

    private void normalize(AgentEnhancer agentEnhancer) {
        agentEnhancer.setAgentEnhancerCode(agentEnhancer.getAgentEnhancerCode().trim());
        agentEnhancer.setAgentEnhancerName(agentEnhancer.getAgentEnhancerName().trim());
        agentEnhancer.setAgentEnhancerType(agentEnhancer.getAgentEnhancerType().trim().toUpperCase(Locale.ROOT));
        agentEnhancer.setEnabled(agentEnhancer.getEnabled() == null ? 1 : (agentEnhancer.getEnabled() == 1 ? 1 : 0));
        if (agentEnhancer.getAgentEnhancerCode().length() > 64) {
            throw new BusinessException("agentEnhancerCode 过长（<=64）");
        }
        if (agentEnhancer.getAgentEnhancerName().length() > 100) {
            throw new BusinessException("agentEnhancerName 过长（<=100）");
        }
        if (agentEnhancer.getConfigJson() != null && agentEnhancer.getConfigJson().length() > 20000) {
            throw new BusinessException("configJson 过大（<=20000）");
        }
    }
}
