package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.aggregate.audit.ConfigAuditAggregate;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.domain.repository.ConfigAuditRepository;
import com.xbk.knowledge.infrastructure.mapper.ConfigAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 配置审计仓储实现
 * 通过 Mapper 执行 XML SQL，隔离持久化细节
 *
 * 职责：仓储实现，用于落地数据访问
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class ConfigAuditRepositoryImpl implements ConfigAuditRepository {

    private final ConfigAuditMapper configAuditMapper;

    /**
     * 保存配置审计
     * 统一补齐创建时间，保证审计可追溯
     *
     * 为什么：保证审计具备时间戳便于追溯
     * 入参：审计聚合
     * 出参：保存后的聚合
     */
    @Override
    public ConfigAuditAggregate save(ConfigAuditAggregate aggregate) {
        if (aggregate == null || aggregate.getConfigAudit() == null) {
            return aggregate;
        }
        ConfigAudit audit = aggregate.getConfigAudit();
        if (audit.getCreatedAt() == null) {
            LocalDateTime createdAt = LocalDateTime.now();
            audit.setCreatedAt(createdAt);
        }
        /*
         * 目的：统一落库入口，避免重复插入逻辑
         */
        configAuditMapper.insertConfigAudit(audit);
        aggregate.setConfigAudit(audit);
        return aggregate;
    }

    /**
     * 按条件分页查询审计
     * 由上层控制排序字段，避免 SQL 注入风险
     *
     * 为什么：分页查询控制返回大小
     * 入参：审计查询条件
     * 出参：审计记录列表
     */
    @Override
    public List<ConfigAudit> findByConditions(AuditQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return configAuditMapper.findByConditions(query);
    }

    /**
     * 统计审计记录总数
     * 用于分页统计
     *
     * 为什么：分页展示需要总数
     * 入参：审计查询条件
     * 出参：总数
     */
    @Override
    public long countByConditions(AuditQuery query) {
        if (query == null) {
            return 0L;
        }
        return configAuditMapper.countByConditions(query);
    }

    /**
     * 查询所有可用表名
     *
     * 为什么：提供筛选下拉数据源
     * 入参：无
     * 出参：表名列表
     */
    @Override
    public List<String> listTableNames() {
        List<String> tableNames = configAuditMapper.listTableNames();
        return tableNames == null ? Collections.emptyList() : tableNames;
    }
}
