package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AdvisorBindingAppService;
import com.xbk.knowledge.application.service.runtime.AdvisorRuntimeService;
import com.xbk.knowledge.domain.advisor.model.entity.AdvisorBinding;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingQuery;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingView;
import com.xbk.knowledge.domain.advisor.adapter.repository.AdvisorBindingRepository;
import com.xbk.knowledge.domain.advisor.adapter.repository.AdvisorRepository;
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
 * Advisor 绑定控制面应用服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class AdvisorBindingAppServiceImpl implements AdvisorBindingAppService {

    private final AdvisorRepository advisorRepository;
    private final AdvisorBindingRepository advisorBindingRepository;
    private final AdvisorRuntimeService advisorRuntimeService;

    /**
     * 根据筛选条件查询Advisor 绑定列表。
     *
     * @param query 查询条件
     * @return 返回 AdvisorBindingView 列表数据。
     */
    @Override
    public List<AdvisorBindingView> listBindings(AdvisorBindingQuery query) {
        if (query == null || !StringUtils.hasText(query.bindType()) || query.bindTargetId() == null) {
            throw new IllegalArgumentException("bindType/bindTargetId 不能为空");
        }
        AdvisorBindingQuery normalized = new AdvisorBindingQuery(
                query.bindType().trim().toUpperCase(Locale.ROOT),
                query.bindTargetId()
        );
        return advisorBindingRepository.listBindingViews(normalized);
    }

    /**
     * 创建或更新Advisor 绑定数据。
     *
     * @param bindType 绑定类型
     * @param bindTargetId 绑定目标 ID
     * @param items 条目列表。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBindings(String bindType, Long bindTargetId, List<AdvisorBindingSaveItem> items) {
        if (!StringUtils.hasText(bindType) || bindTargetId == null) {
            throw new IllegalArgumentException("bindType/bindTargetId 不能为空");
        }
        String bt = bindType.trim().toUpperCase(Locale.ROOT);
        if (!("AGENT_VERSION".equals(bt) || "WORKFLOW_VERSION".equals(bt))) {
            throw new BusinessException("bindType 非法，仅支持 AGENT_VERSION/WORKFLOW_VERSION");
        }

        AdvisorBindingQuery q = new AdvisorBindingQuery(bt, bindTargetId);
        advisorBindingRepository.deleteByTarget(q);
        advisorRuntimeService.evictBindingCache(bt, bindTargetId);

        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        List<AdvisorBindingSaveItem> safe = new ArrayList<>();
        for (AdvisorBindingSaveItem it : items) {
            if (it == null || it.getAdvisorId() == null) {
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
        for (AdvisorBindingSaveItem it : safe) {
            Long advisorId = it.getAdvisorId();
            if (!seen.add(advisorId)) {
                continue;
            }
            advisorRepository.findById(advisorId)
                    .orElseThrow(() -> new BusinessException("Advisor 不存在，advisorId=" + advisorId));
            Integer orderNo = it.getOrderNo() != null ? it.getOrderNo() : idx;
            boolean enabled = it.getEnabled() == null || it.getEnabled();
            AdvisorBinding binding = AdvisorBinding.builder()
                    .bindType(bt)
                    .bindTargetId(bindTargetId)
                    .advisorId(advisorId)
                    .orderNo(orderNo)
                    .enabled(enabled ? 1 : 0)
                    .build();
            advisorBindingRepository.insertBinding(binding);
            idx++;
        }
    }
}
