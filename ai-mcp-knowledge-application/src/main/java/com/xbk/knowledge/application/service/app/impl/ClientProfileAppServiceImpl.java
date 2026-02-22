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
     * queryPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public PageResult<ClientProfile> queryPage(ClientProfilePageQuery query) {
        return clientProfileService.queryPage(query);
    }

    /**
     * get。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public ClientProfile get(Long id) {
        return clientProfileService.get(id);
    }

    /**
     * save。
     *
     * @param profile 参数
     * @param steps 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientProfile save(ClientProfile profile, List<ClientProfileStep> steps) {
        return clientProfileService.save(profile, steps);
    }

    /**
     * enable。
     *
     * @param id 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientProfile enable(Long id, Long updatedBy) {
        return clientProfileService.enable(id, updatedBy);
    }

    /**
     * disable。
     *
     * @param id 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientProfile disable(Long id, Long updatedBy) {
        return clientProfileService.disable(id, updatedBy);
    }

    /**
     * remove。
     *
     * @param id 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        clientProfileService.remove(id);
    }

    /**
     * listSteps。
     *
     * @param clientProfileId 参数
     * @return 返回结果
     */
    @Override
    public List<ClientProfileStep> listSteps(Long clientProfileId) {
        return clientProfileService.listSteps(clientProfileId);
    }
}
