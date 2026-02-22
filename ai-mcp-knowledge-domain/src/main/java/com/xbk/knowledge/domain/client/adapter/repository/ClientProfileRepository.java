package com.xbk.knowledge.domain.client.adapter.repository;

import com.xbk.knowledge.domain.client.model.entity.ClientProfile;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.client.model.valobj.ClientProfilePageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Client Profile 仓储接口。
 *
 * @author sxie
 */
public interface ClientProfileRepository {

    Optional<ClientProfile> findById(Long id);

    Optional<ClientProfile> findByCode(String clientCode);

    List<ClientProfile> findPage(ClientProfilePageQuery query);

    long count(ClientProfilePageQuery query);

    ClientProfile insert(ClientProfile profile);

    int update(ClientProfile profile);

    int removeById(Long id);

    List<ClientProfileStep> listSteps(Long clientProfileId);

    int deleteStepsByProfileId(Long clientProfileId);

    int batchInsertSteps(List<ClientProfileStep> steps);
}
