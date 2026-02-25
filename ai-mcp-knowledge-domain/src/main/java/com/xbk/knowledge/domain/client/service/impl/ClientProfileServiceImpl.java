package com.xbk.knowledge.domain.client.service.impl;

import com.xbk.knowledge.domain.client.adapter.repository.ClientProfileRepository;
import com.xbk.knowledge.domain.client.model.entity.ClientProfile;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.client.model.valobj.ClientProfilePageQuery;
import com.xbk.knowledge.domain.client.service.IClientProfileService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client Profile 领域服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class ClientProfileServiceImpl implements IClientProfileService {

    /**
     * 客户端画像仓储。
     */
    private final ClientProfileRepository clientProfileRepository;

    /**
     * 查询分页数据。
     * 
     * @param query 分页查询条件。
     * @return 客户画像分页结果。
     */
    @Override
    public PageResult<ClientProfile> queryPage(ClientProfilePageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query 不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        ClientProfilePageQuery normalized = ClientProfilePageQuery.builder()
                .keyword(query.getKeyword())
                .status(query.getStatus())
                .offset(offset)
                .pageSize(pageSize)
                .build();
        List<ClientProfile> records = clientProfileRepository.findPage(normalized);
        long total = clientProfileRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 获取业务数据。
     * 
     * @param id 主键ID。
     * @return 客户画像详情。
     */
    @Override
    public ClientProfile get(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        return clientProfileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ClientProfile 不存在，id=" + id));
    }

    /**
     * 保存业务数据。
     * 
     * @param profile 客户画像实体。
     * @param steps 步骤列表。
     * @return 保存后的客户画像。
     */
    @Override
    public ClientProfile save(ClientProfile profile, List<ClientProfileStep> steps) {
        if (profile == null) {
            throw new IllegalArgumentException("profile 不能为空");
        }
        if (profile.getClientCode() == null || profile.getClientCode().isBlank()) {
            throw new IllegalArgumentException("clientCode 不能为空");
        }
        if (profile.getClientName() == null || profile.getClientName().isBlank()) {
            throw new IllegalArgumentException("clientName 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        if (profile.getStatus() == null || profile.getStatus().isBlank()) {
            profile.setStatus("ENABLED");
        }

        ClientProfile saved;
        if (profile.getId() == null) {
            if (clientProfileRepository.findByCode(profile.getClientCode()).isPresent()) {
                throw new BusinessException("clientCode 已存在" + profile.getClientCode());
            }
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);
            saved = clientProfileRepository.insert(profile);
        } else {
            ClientProfile existed = get(profile.getId());
            if (!existed.getClientCode().equals(profile.getClientCode())
                    && clientProfileRepository.findByCode(profile.getClientCode()).isPresent()) {
                throw new BusinessException("clientCode 已存在" + profile.getClientCode());
            }
            existed.setClientCode(profile.getClientCode());
            existed.setClientName(profile.getClientName());
            existed.setDescription(profile.getDescription());
            existed.setStatus(profile.getStatus());
            existed.setUpdatedBy(profile.getUpdatedBy());
            existed.setUpdatedAt(now);
            int affected = clientProfileRepository.update(existed);
            if (affected <= 0) {
                throw new BusinessException("ClientProfile 更新失败，id=" + profile.getId());
            }
            saved = get(profile.getId());
        }

        replaceSteps(saved.getId(), steps);
        return get(saved.getId());
    }

    @Override
    public ClientProfile enable(Long id, Long updatedBy) {
        ClientProfile profile = get(id);
        profile.setStatus("ENABLED");
        profile.setUpdatedBy(updatedBy);
        profile.setUpdatedAt(LocalDateTime.now());
        clientProfileRepository.update(profile);
        return get(id);
    }

    @Override
    public ClientProfile disable(Long id, Long updatedBy) {
        ClientProfile profile = get(id);
        profile.setStatus("DISABLED");
        profile.setUpdatedBy(updatedBy);
        profile.setUpdatedAt(LocalDateTime.now());
        clientProfileRepository.update(profile);
        return get(id);
    }

    @Override
    public void remove(Long id) {
        get(id);
        clientProfileRepository.deleteStepsByProfileId(id);
        int affected = clientProfileRepository.removeById(id);
        if (affected <= 0) {
            throw new BusinessException("ClientProfile 删除失败，id=" + id);
        }
    }

    /**
     * 查询步骤列表。
     * 
     * @param clientProfileId 客户端画像ID。
     * @return 步骤集合。
     */
    @Override
    public List<ClientProfileStep> listSteps(Long clientProfileId) {
        if (clientProfileId == null) {
            return List.of();
        }
        return clientProfileRepository.listSteps(clientProfileId);
    }

    private void replaceSteps(Long clientProfileId, List<ClientProfileStep> steps) {
        clientProfileRepository.deleteStepsByProfileId(clientProfileId);
        if (steps == null || steps.isEmpty()) {
            return;
        }
        List<ClientProfileStep> normalized = new ArrayList<>();
        int idx = 0;
        for (ClientProfileStep step : steps) {
            if (step == null) {
                continue;
            }
            idx++;
            Integer seq = step.getSequenceNo();
            if (seq == null || seq <= 0) {
                seq = idx;
            }
            if (step.getModelId() == null) {
                throw new BusinessException("ClientProfile 步骤缺少 modelId，sequence=" + seq);
            }
            ClientProfileStep s = ClientProfileStep.builder()
                    .clientProfileId(clientProfileId)
                    .sequenceNo(seq)
                    .stepName(step.getStepName())
                    .modelId(step.getModelId())
                    .systemPrompt(step.getSystemPrompt())
                    .enableTools(step.getEnableTools() == null || step.getEnableTools())
                    .allowedToolKeysJson(step.getAllowedToolKeysJson())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            normalized.add(s);
        }
        normalized.sort(Comparator.comparingInt(ClientProfileStep::getSequenceNo));
        if (!normalized.isEmpty()) {
            clientProfileRepository.batchInsertSteps(normalized);
        }
    }
}
