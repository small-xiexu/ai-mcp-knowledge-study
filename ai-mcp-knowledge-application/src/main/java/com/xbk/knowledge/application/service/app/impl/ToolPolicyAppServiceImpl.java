package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.ToolPolicyAppService;
import com.xbk.knowledge.domain.model.entity.tool.ToolPolicy;
import com.xbk.knowledge.domain.model.vo.tool.ToolPolicyPageQuery;
import com.xbk.knowledge.domain.repository.ToolPolicyRepository;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * ToolPolicy 应用服务实现。
 *
 * 说明：用于治理控制面配置（按 org 隔离）。
 *
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class ToolPolicyAppServiceImpl implements ToolPolicyAppService {

    private final ToolPolicyRepository toolPolicyRepository;

    @Override
    public PageResult<ToolPolicy> queryPage(ToolPolicyPageQuery query) {
        if (query == null || query.orgId() == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        int offset = query.offset() == null ? 0 : Math.max(query.offset(), 0);
        int pageSize = query.pageSize() == null ? 20 : Math.min(Math.max(query.pageSize(), 1), 200);
        ToolPolicyPageQuery normalized = new ToolPolicyPageQuery(
                query.orgId(),
                StringUtils.hasText(query.keyword()) ? query.keyword().trim() : null,
                query.enabled(),
                offset,
                pageSize
        );
        List<ToolPolicy> records = toolPolicyRepository.findPage(normalized);
        long total = toolPolicyRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    @Override
    public ToolPolicy get(Long orgId, Long id) {
        if (orgId == null || id == null) {
            throw new IllegalArgumentException("orgId/id 不能为空");
        }
        return toolPolicyRepository.findById(orgId, id)
                .orElseThrow(() -> new NotFoundException("工具策略不存在，id=" + id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToolPolicy save(ToolPolicy policy) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        if (policy == null || policy.getOrgId() == null) {
            throw new IllegalArgumentException("orgId 不能为空");
        }
        if (!StringUtils.hasText(policy.getToolKey())) {
            throw new BusinessException("toolKey 不能为空");
        }
        normalize(policy);

        // id 优先（更新）
        if (policy.getId() != null) {
            ToolPolicy existed = get(policy.getOrgId(), policy.getId());
            existed.setRiskLevel(policy.getRiskLevel());
            existed.setApprovalRequired(policy.getApprovalRequired());
            existed.setEnabled(policy.getEnabled());
            existed.setRemark(policy.getRemark());
            int affected = toolPolicyRepository.update(existed);
            if (affected <= 0) {
                throw new BusinessException("更新失败，id=" + policy.getId());
            }
            return get(policy.getOrgId(), policy.getId());
        }

        // 否则按 toolKey upsert
        ToolPolicy existed = toolPolicyRepository.findByToolKey(policy.getOrgId(), policy.getToolKey()).orElse(null);
        if (existed == null || existed.getId() == null) {
            toolPolicyRepository.insert(policy);
            return get(policy.getOrgId(), policy.getId());
        }
        existed.setRiskLevel(policy.getRiskLevel());
        existed.setApprovalRequired(policy.getApprovalRequired());
        existed.setEnabled(policy.getEnabled());
        existed.setRemark(policy.getRemark());
        int affected = toolPolicyRepository.update(existed);
        if (affected <= 0) {
            throw new BusinessException("保存失败，toolKey=" + policy.getToolKey());
        }
        return get(policy.getOrgId(), existed.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToolPolicy enable(Long orgId, Long id) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        ToolPolicy existed = get(orgId, id);
        int affected = toolPolicyRepository.updateEnabled(orgId, id, 1);
        if (affected <= 0) {
            throw new BusinessException("启用失败，id=" + id);
        }
        return get(orgId, existed.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToolPolicy disable(Long orgId, Long id) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        ToolPolicy existed = get(orgId, id);
        int affected = toolPolicyRepository.updateEnabled(orgId, id, 0);
        if (affected <= 0) {
            throw new BusinessException("禁用失败，id=" + id);
        }
        return get(orgId, existed.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long orgId, Long id) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        get(orgId, id);
        int affected = toolPolicyRepository.deleteById(orgId, id);
        if (affected <= 0) {
            throw new BusinessException("删除失败，id=" + id);
        }
    }

    private void normalize(ToolPolicy policy) {
        String risk = StringUtils.hasText(policy.getRiskLevel()) ? policy.getRiskLevel().trim() : "MEDIUM";
        risk = risk.toUpperCase(Locale.ROOT);
        if (!("LOW".equals(risk) || "MEDIUM".equals(risk) || "HIGH".equals(risk))) {
            throw new BusinessException("riskLevel 非法，仅支持 LOW/MEDIUM/HIGH");
        }
        policy.setRiskLevel(risk);
        policy.setApprovalRequired(policy.getApprovalRequired() == null ? 0 : (policy.getApprovalRequired() == 1 ? 1 : 0));
        policy.setEnabled(policy.getEnabled() == null ? 1 : (policy.getEnabled() == 1 ? 1 : 0));
        if (policy.getRemark() != null && policy.getRemark().length() > 500) {
            policy.setRemark(policy.getRemark().substring(0, 500));
        }
        policy.setToolKey(policy.getToolKey().trim());
    }
}

