package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.ClientProfileAppService;
import com.xbk.knowledge.domain.client.model.entity.ClientProfile;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.client.model.valobj.ClientProfilePageQuery;
import com.xbk.knowledge.domain.client.service.IClientProfileService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Client Profile 控制面应用服务实现。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class ClientProfileAppServiceImpl implements ClientProfileAppService {

    private final IClientProfileService clientProfileService;

    /**
     * 查询客户端画像。
     *
     * @param query 查询条件
     * @return 返回 ClientProfile 分页数据。
     */
    @Override
    public PageResult<ClientProfile> queryPage(ClientProfilePageQuery query) {
        return clientProfileService.queryPage(query);
    }

    /**
     * 查询客户端画像。
     *
     * @param id 主键 ID
     * @return 返回 ClientProfile 数据。
     */
    @Override
    public ClientProfile get(Long id) {
        return clientProfileService.get(id);
    }

    /**
     * 根据筛选条件查询客户端画像列表。
     *
     * @param profile 客户端画像配置。
     * @param steps 步骤列表。
     * @return 返回 ClientProfile 数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientProfile save(ClientProfile profile, List<ClientProfileStep> steps) {
        return clientProfileService.save(profile, steps);
    }

    /**
     * 根据筛选条件查询客户端画像列表。
     *
     * @param id 主键 ID
     * @param updatedBy 更新人 ID
     * @return 返回 ClientProfile 数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientProfile enable(Long id, Long updatedBy) {
        return clientProfileService.enable(id, updatedBy);
    }

    /**
     * 根据筛选条件查询客户端画像列表。
     *
     * @param id 主键 ID
     * @param updatedBy 更新人 ID
     * @return 返回 ClientProfile 数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientProfile disable(Long id, Long updatedBy) {
        return clientProfileService.disable(id, updatedBy);
    }

    /**
     * 根据筛选条件查询客户端画像列表。
     *
     * @param id 主键 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        clientProfileService.remove(id);
    }

    /**
     * 根据筛选条件查询客户端画像列表。
     *
     * @param clientProfileId 客户端画像 ID。
     * @return 返回 ClientProfileStep 列表数据。
     */
    @Override
    public List<ClientProfileStep> listSteps(Long clientProfileId) {
        return clientProfileService.listSteps(clientProfileId);
    }
}
