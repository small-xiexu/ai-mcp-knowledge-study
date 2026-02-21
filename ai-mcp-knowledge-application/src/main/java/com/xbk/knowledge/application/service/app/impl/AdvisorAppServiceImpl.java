package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AdvisorAppService;
import com.xbk.knowledge.application.service.runtime.AdvisorRuntimeService;
import com.xbk.knowledge.domain.advisor.model.entity.Advisor;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorPageQuery;
import com.xbk.knowledge.domain.advisor.adapter.repository.AdvisorBindingRepository;
import com.xbk.knowledge.domain.advisor.adapter.repository.AdvisorRepository;
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
 * Advisor 控制面应用服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class AdvisorAppServiceImpl implements AdvisorAppService {

    private final AdvisorRepository advisorRepository;
    private final AdvisorBindingRepository advisorBindingRepository;
    private final AdvisorRuntimeService advisorRuntimeService;

    /**
     * queryPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public PageResult<Advisor> queryPage(AdvisorPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query 不能为空");
        }
        int offset = query.offset() == null ? 0 : Math.max(query.offset(), 0);
        int pageSize = query.pageSize() == null ? 20 : Math.min(Math.max(query.pageSize(), 1), 200);
        AdvisorPageQuery normalized = new AdvisorPageQuery(
                StringUtils.hasText(query.keyword()) ? query.keyword().trim() : null,
                query.enabled(),
                StringUtils.hasText(query.advisorType()) ? query.advisorType().trim().toUpperCase(Locale.ROOT) : null,
                offset,
                pageSize
        );
        List<Advisor> records = advisorRepository.findPage(normalized);
        long total = advisorRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * get。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Advisor get(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        return advisorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advisor 不存在，id=" + id));
    }

    /**
     * save。
     *
     * @param advisor 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Advisor save(Advisor advisor) {
        if (advisor == null) {
            throw new IllegalArgumentException("advisor 不能为空");
        }
        if (!StringUtils.hasText(advisor.getAdvisorCode())) {
            throw new BusinessException("advisorCode 不能为空");
        }
        if (!StringUtils.hasText(advisor.getAdvisorName())) {
            throw new BusinessException("advisorName 不能为空");
        }
        if (!StringUtils.hasText(advisor.getAdvisorType())) {
            throw new BusinessException("advisorType 不能为空");
        }
        normalize(advisor);

        if (advisor.getId() != null) {
            Advisor existed = get(advisor.getId());
            existed.setAdvisorCode(advisor.getAdvisorCode());
            existed.setAdvisorName(advisor.getAdvisorName());
            existed.setAdvisorType(advisor.getAdvisorType());
            existed.setEnabled(advisor.getEnabled());
            existed.setConfigJson(advisor.getConfigJson());
            int affected = advisorRepository.update(existed);
            if (affected <= 0) {
                throw new BusinessException("更新失败，id=" + advisor.getId());
            }
            advisorRuntimeService.evictAll();
            return get(existed.getId());
        }

        Advisor existed = advisorRepository.findByCode(advisor.getAdvisorCode()).orElse(null);
        if (existed == null || existed.getId() == null) {
            advisorRepository.insert(advisor);
            advisorRuntimeService.evictAll();
            return get(advisor.getId());
        }
        existed.setAdvisorName(advisor.getAdvisorName());
        existed.setAdvisorType(advisor.getAdvisorType());
        existed.setEnabled(advisor.getEnabled());
        existed.setConfigJson(advisor.getConfigJson());
        int affected = advisorRepository.update(existed);
        if (affected <= 0) {
            throw new BusinessException("保存失败，advisorCode=" + advisor.getAdvisorCode());
        }
        advisorRuntimeService.evictAll();
        return get(existed.getId());
    }

    /**
     * enable。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Advisor enable(Long id) {
        Advisor existed = get(id);
        int affected = advisorRepository.updateEnabled(id, 1);
        if (affected <= 0) {
            throw new BusinessException("启用失败，id=" + id);
        }
        advisorRuntimeService.evictAll();
        return get(existed.getId());
    }

    /**
     * disable。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Advisor disable(Long id) {
        Advisor existed = get(id);
        int affected = advisorRepository.updateEnabled(id, 0);
        if (affected <= 0) {
            throw new BusinessException("禁用失败，id=" + id);
        }
        advisorRuntimeService.evictAll();
        return get(existed.getId());
    }

    /**
     * remove。
     *
     * @param id 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Advisor existed = get(id);
        advisorBindingRepository.deleteByAdvisorId(existed.getId());
        int affected = advisorRepository.deleteById(id);
        if (affected <= 0) {
            throw new BusinessException("删除失败，id=" + id);
        }
        advisorRuntimeService.evictAll();
    }

    private void normalize(Advisor advisor) {
        advisor.setAdvisorCode(advisor.getAdvisorCode().trim());
        advisor.setAdvisorName(advisor.getAdvisorName().trim());
        advisor.setAdvisorType(advisor.getAdvisorType().trim().toUpperCase(Locale.ROOT));
        advisor.setEnabled(advisor.getEnabled() == null ? 1 : (advisor.getEnabled() == 1 ? 1 : 0));
        if (advisor.getAdvisorCode().length() > 64) {
            throw new BusinessException("advisorCode 过长（<=64）");
        }
        if (advisor.getAdvisorName().length() > 100) {
            throw new BusinessException("advisorName 过长（<=100）");
        }
        if (advisor.getConfigJson() != null && advisor.getConfigJson().length() > 20000) {
            throw new BusinessException("configJson 过大（<=20000）");
        }
    }
}
