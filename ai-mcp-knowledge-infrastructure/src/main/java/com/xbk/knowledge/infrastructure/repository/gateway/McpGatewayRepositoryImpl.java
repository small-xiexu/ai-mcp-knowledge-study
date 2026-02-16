package com.xbk.knowledge.infrastructure.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayPageQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayRepository;
import com.xbk.knowledge.infrastructure.mapper.gateway.McpGatewayMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Gateway 实例仓储实现
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class McpGatewayRepositoryImpl implements McpGatewayRepository {

    private final McpGatewayMapper mapper;

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

    /**
     * findByGatewayId。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<McpGateway> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Optional.empty();
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return Optional.ofNullable(mapper.findByGatewayId(query));
    }

    /**
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<McpGateway> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return Optional.ofNullable(mapper.findById(query));
    }

    /**
     * save。
     *
     * @param gateway 参数
     * @return 返回结果
     */
    @Override
    public McpGateway save(McpGateway gateway) {
        if (gateway == null) {
            return null;
        }
        if (gateway.getId() == null) {
            mapper.insertGateway(gateway);
            return gateway;
        }
        mapper.updateGateway(gateway);
        return gateway;
    }

    /**
     * deleteById。
     *
     * @param query 参数
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        mapper.deleteGatewayById(query);
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<McpGateway> findPage(GatewayPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return mapper.findPage(query);
    }

    /**
     * findAllEnabled。
     *
     * @return 返回结果
     */
    @Override
    public List<McpGateway> findAllEnabled() {
        return mapper.findAllEnabledByOrgId(currentOrgIdOrRoot());
    }

    /**
     * countAll。
     *
     * @return 返回结果
     */
    @Override
    public long countAll() {
        return mapper.countAllByOrgId(currentOrgIdOrRoot());
    }
}
