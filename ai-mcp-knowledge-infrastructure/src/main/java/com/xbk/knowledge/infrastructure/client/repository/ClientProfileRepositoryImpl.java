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

    /**
     * 客户端画像数据访问对象。
     */
    private final IClientProfileDao clientProfileDao;

    /**
     * 客户端画像步骤数据访问对象。
     */
    private final IClientProfileStepDao clientProfileStepDao;

    /**
     * 查询客户端画像。
     * 
     * @param id 主键 ID
     * @return ClientProfile 查询结果（可能为空）。
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
     * 查询客户端画像。
     * 
     * @param clientCode 客户端编码。
     * @return ClientProfile 查询结果（可能为空）。
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
     * 查询客户端画像。
     * 
     * @param query 分页查询条件。
     * @return ClientProfile 列表数据。
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
     * 按条件统计业务数量。
     * 
     * @param query 分页查询条件。
     * @return 统计数量
     */
    @Override
    public long count(ClientProfilePageQuery query) {
        if (query == null) {
            return 0L;
        }
        return clientProfileDao.count(query);
    }

    /**
     * 创建并持久化客户端画像数据。
     * 
     * @param profile 客户端画像配置。
     * @return ClientProfile 数据。
     */
    @Override
    public ClientProfile insert(ClientProfile profile) {
        ClientProfilePO po = BeanMappingUtils.map(profile, ClientProfilePO.class);
        clientProfileDao.insertClientProfile(po);
        return BeanMappingUtils.map(po, ClientProfile.class);
    }

    /**
     * 更新客户端画像数据。
     * 
     * @param profile 客户端画像配置。
     * @return 画像更新条数。
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
     * 删除客户端画像数据。
     * 
     * @param id 主键 ID
     * @return 画像删除条数。
     */
    @Override
    public int removeById(Long id) {
        if (id == null) {
            return 0;
        }
        return clientProfileDao.deleteById(id);
    }

    /**
     * 根据筛选条件查询客户端画像列表。
     * 
     * @param clientProfileId 客户端画像 ID。
     * @return ClientProfileStep 列表数据。
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
     * 删除客户端画像数据。
     * 
     * @param clientProfileId 客户端画像 ID。
     * @return 步骤删除条数。
     */
    @Override
    public int deleteStepsByProfileId(Long clientProfileId) {
        if (clientProfileId == null) {
            return 0;
        }
        return clientProfileStepDao.deleteByClientProfileId(clientProfileId);
    }

    /**
     * 批量插入客户端画像步骤。
     * 
     * @param steps 步骤列表。
     * @return 步骤新增条数。
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
