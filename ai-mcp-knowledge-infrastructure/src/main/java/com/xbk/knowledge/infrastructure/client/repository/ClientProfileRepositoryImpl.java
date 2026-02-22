package com.xbk.knowledge.infrastructure.client.repository;

import com.xbk.knowledge.domain.client.adapter.repository.ClientProfileRepository;
import com.xbk.knowledge.domain.client.model.entity.ClientProfile;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.client.model.valobj.ClientProfilePageQuery;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IClientProfileDao;
import com.xbk.knowledge.infrastructure.dao.IClientProfileStepDao;
import com.xbk.knowledge.infrastructure.dao.po.ClientProfilePO;
import com.xbk.knowledge.infrastructure.dao.po.ClientProfileStepPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Client Profile 仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class ClientProfileRepositoryImpl implements ClientProfileRepository {

    private final IClientProfileDao clientProfileDao;
    private final IClientProfileStepDao clientProfileStepDao;

    /**
     * findById。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Optional<ClientProfile> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        ClientProfilePO po = clientProfileDao.findById(id);
        return Optional.ofNullable(BeanMappingUtils.map(po, ClientProfile.class));
    }

    /**
     * findByCode。
     *
     * @param clientCode 参数
     * @return 返回结果
     */
    @Override
    public Optional<ClientProfile> findByCode(String clientCode) {
        if (clientCode == null || clientCode.isBlank()) {
            return Optional.empty();
        }
        ClientProfilePO po = clientProfileDao.findByCode(clientCode);
        return Optional.ofNullable(BeanMappingUtils.map(po, ClientProfile.class));
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<ClientProfile> findPage(ClientProfilePageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        List<ClientProfilePO> records = clientProfileDao.findPage(query);
        return BeanMappingUtils.mapList(records, ClientProfile.class);
    }

    /**
     * count。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long count(ClientProfilePageQuery query) {
        if (query == null) {
            return 0L;
        }
        return clientProfileDao.count(query);
    }

    /**
     * insert。
     *
     * @param profile 参数
     * @return 返回结果
     */
    @Override
    public ClientProfile insert(ClientProfile profile) {
        ClientProfilePO po = BeanMappingUtils.map(profile, ClientProfilePO.class);
        clientProfileDao.insertClientProfile(po);
        return BeanMappingUtils.map(po, ClientProfile.class);
    }

    /**
     * update。
     *
     * @param profile 参数
     * @return 返回结果
     */
    @Override
    public int update(ClientProfile profile) {
        if (profile == null || profile.getId() == null) {
            return 0;
        }
        ClientProfilePO po = BeanMappingUtils.map(profile, ClientProfilePO.class);
        return clientProfileDao.updateClientProfile(po);
    }

    /**
     * removeById。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public int removeById(Long id) {
        if (id == null) {
            return 0;
        }
        return clientProfileDao.deleteById(id);
    }

    /**
     * listSteps。
     *
     * @param clientProfileId 参数
     * @return 返回结果
     */
    @Override
    public List<ClientProfileStep> listSteps(Long clientProfileId) {
        if (clientProfileId == null) {
            return Collections.emptyList();
        }
        List<ClientProfileStepPO> records = clientProfileStepDao.listByClientProfileId(clientProfileId);
        return BeanMappingUtils.mapList(records, ClientProfileStep.class);
    }

    /**
     * deleteStepsByProfileId。
     *
     * @param clientProfileId 参数
     * @return 返回结果
     */
    @Override
    public int deleteStepsByProfileId(Long clientProfileId) {
        if (clientProfileId == null) {
            return 0;
        }
        return clientProfileStepDao.deleteByClientProfileId(clientProfileId);
    }

    /**
     * batchInsertSteps。
     *
     * @param steps 参数
     * @return 返回结果
     */
    @Override
    public int batchInsertSteps(List<ClientProfileStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return 0;
        }
        List<ClientProfileStepPO> records = BeanMappingUtils.mapList(steps, ClientProfileStepPO.class);
        return clientProfileStepDao.batchInsert(records);
    }
}
