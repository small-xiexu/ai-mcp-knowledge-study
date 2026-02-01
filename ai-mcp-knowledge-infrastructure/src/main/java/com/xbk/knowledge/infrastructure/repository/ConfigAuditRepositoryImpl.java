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
        configAuditMapper.insertConfigAudit(audit);
        aggregate.setConfigAudit(audit);
        return aggregate;
    }

    /**
     * 按条件分页查询审计
     * 由上层控制排序字段，避免 SQL 注入风险
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
     */
    @Override
    public List<String> listTableNames() {
        List<String> tableNames = configAuditMapper.listTableNames();
        return tableNames == null ? Collections.emptyList() : tableNames;
    }
}
