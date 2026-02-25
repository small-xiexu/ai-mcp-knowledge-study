package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayPageQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IMcpGatewayDao;
import com.xbk.knowledge.infrastructure.dao.po.McpGatewayPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Gateway 实例仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpGatewayRepositoryImpl implements McpGatewayRepository {

    /**
     * Gateway DAO。
     */
    private final IMcpGatewayDao mapper;

    /**
     * 查询MCP 网关。
     *
     * @param query 主键查询条件
     * @return McpGateway 查询结果（可能为空）
     */
    @Override
    public Optional<McpGateway> findByGatewayId(GatewayIdQuery query) {
        if (query == null || query.getGatewayId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByGatewayId(query))
                .map(item -> BeanMappingUtils.map(item, McpGateway.class));
    }

    /**
     * 查询MCP 网关。
     *
     * @param query 主键查询条件
     * @return McpGateway 查询结果（可能为空）
     */
    @Override
    public Optional<McpGateway> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(query))
                .map(item -> BeanMappingUtils.map(item, McpGateway.class));
    }

    /**
     * 创建或更新MCP 网关数据。
     *
     * @param gateway 网关配置
     * @return 保存后的 McpGateway 信息
     */
    @Override
    public McpGateway save(McpGateway gateway) {
        if (gateway == null) {
            return null;
        }
        if (gateway.getId() == null) {
            mapper.insertGateway(BeanMappingUtils.map(gateway, McpGatewayPO.class));
            return gateway;
        }
        mapper.updateGateway(BeanMappingUtils.map(gateway, McpGatewayPO.class));
        return gateway;
    }

    /**
     * 删除MCP 网关数据。
     *
     * @param query 主键查询条件
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        mapper.deleteGatewayById(query);
    }

    /**
     * 查询MCP 网关。
     *
     * @param query 分页查询条件
     * @return McpGateway 列表
     */
    @Override
    public List<McpGateway> findPage(GatewayPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findPage(query), McpGateway.class);
    }

    /**
     * 查询MCP 网关。
     *
     * @return 已启用 McpGateway 列表
     */
    @Override
    public List<McpGateway> findAllEnabled() {
        return BeanMappingUtils.mapList(mapper.findAllEnabled(), McpGateway.class);
    }

    /**
     * 按条件统计业务数量。
     *
     * @return 统计数量
     */
    @Override
    public long countAll() {
        return mapper.countAll();
    }
}
