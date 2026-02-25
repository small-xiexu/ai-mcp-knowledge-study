package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AgentEnhancerBindingAppService;
import com.xbk.knowledge.application.service.runtime.AgentEnhancerRuntimeService;
import com.xbk.knowledge.domain.agentenhancer.model.entity.AgentEnhancerBinding;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingQuery;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingView;
import com.xbk.knowledge.domain.agentenhancer.adapter.repository.AgentEnhancerBindingRepository;
import com.xbk.knowledge.domain.agentenhancer.adapter.repository.AgentEnhancerRepository;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AgentEnhancer 绑定控制面应用服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class AgentEnhancerBindingAppServiceImpl implements AgentEnhancerBindingAppService {

    /**
     * Agent 增强器仓储。
     */
    private final AgentEnhancerRepository agentEnhancerRepository;

    /**
     * Agent 增强器绑定仓储。
     */
    private final AgentEnhancerBindingRepository agentEnhancerBindingRepository;

    /**
     * Agent 增强器运行时服务。
     */
    private final AgentEnhancerRuntimeService agentEnhancerRuntimeService;

    /**
     * 根据筛选条件查询AgentEnhancer 绑定列表。
     * 
     * @param query AgentEnhancer 绑定查询条件。
     * @return AgentEnhancerBindingView 列表数据。
     */
    @Override
    public List<AgentEnhancerBindingView> listBindings(AgentEnhancerBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            throw new IllegalArgumentException("bindType/bindTargetId 不能为空");
        }
        AgentEnhancerBindingQuery normalized = new AgentEnhancerBindingQuery(
                query.bindType().trim().toUpperCase(Locale.ROOT),
                query.bindTargetId()
        );
        return agentEnhancerBindingRepository.listBindingViews(normalized);
    }

    /**
     * 创建或更新AgentEnhancer 绑定数据。
     * 
     * @param bindType 绑定类型
     * @param bindTargetId 绑定目标 ID
     * @param items 条目列表。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBindings(String bindType, Long bindTargetId, List<AgentEnhancerBindingSaveItem> items) {
        if (!StringUtils.hasText(bindType) || bindTargetId == null) {
            throw new IllegalArgumentException("bindType/bindTargetId 不能为空");
        }
        String bt = bindType.trim().toUpperCase(Locale.ROOT);
        if (!("AGENT_VERSION".equals(bt) || "WORKFLOW_VERSION".equals(bt))) {
            throw new BusinessException("bindType 非法，仅支持 AGENT_VERSION/WORKFLOW_VERSION");
        }

        AgentEnhancerBindingQuery q = new AgentEnhancerBindingQuery(bt, bindTargetId);
        agentEnhancerBindingRepository.deleteByTarget(q);
        agentEnhancerRuntimeService.evictBindingCache(bt, bindTargetId);

        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        List<AgentEnhancerBindingSaveItem> safe = new ArrayList<>();
        for (AgentEnhancerBindingSaveItem it : items) {
            if (it == null || it.getAgentEnhancerId() == null) {
                continue;
            }
            safe.add(it);
        }
        if (safe.isEmpty()) {
            return;
        }

        // 去重 + existence 校验
        Set<Long> seen = new HashSet<>();
        int idx = 0;
        for (AgentEnhancerBindingSaveItem it : safe) {
            Long agentEnhancerId = it.getAgentEnhancerId();
            if (!seen.add(agentEnhancerId)) {
                continue;
            }
            agentEnhancerRepository.findById(agentEnhancerId)
                    .orElseThrow(() -> new BusinessException("AgentEnhancer 不存在，agentEnhancerId=" + agentEnhancerId));
            Integer orderNo = it.getOrderNo() != null ? it.getOrderNo() : idx;
            boolean enabled = it.getEnabled() == null || it.getEnabled();
            AgentEnhancerBinding binding = AgentEnhancerBinding.builder()
                    .bindType(bt)
                    .bindTargetId(bindTargetId)
                    .agentEnhancerId(agentEnhancerId)
                    .orderNo(orderNo)
                    .enabled(enabled ? 1 : 0)
                    .build();
            agentEnhancerBindingRepository.insertBinding(binding);
            idx++;
        }
    }
}
