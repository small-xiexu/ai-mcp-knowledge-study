package com.xbk.knowledge.domain.client.service;

import com.xbk.knowledge.domain.client.model.entity.ClientProfile;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.client.model.valobj.ClientProfilePageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * Client Profile 领域服务。
 *
 * @author sxie
 */
public interface IClientProfileService {

    PageResult<ClientProfile> queryPage(ClientProfilePageQuery query);

    ClientProfile get(Long id);

    ClientProfile save(ClientProfile profile, List<ClientProfileStep> steps);

    ClientProfile enable(Long id, Long updatedBy);

    ClientProfile disable(Long id, Long updatedBy);

    void remove(Long id);

    List<ClientProfileStep> listSteps(Long clientProfileId);
}
