package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.repository.ConfigAuditRepository;
import com.xbk.knowledge.infrastructure.mapper.ConfigAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Override
    public ConfigAudit save(ConfigAudit audit) {
        if (audit.getCreatedAt() == null) {
            audit.setCreatedAt(LocalDateTime.now());
        }
        configAuditMapper.insertConfigAudit(audit);
        return audit;
    }

    @Override
    public List<ConfigAudit> findByTableNameAndRecordId(String tableName, Long recordId) {
        return configAuditMapper.findByTableNameAndRecordId(tableName, recordId);
    }

    @Override
    public List<ConfigAudit> findByOperator(String operator) {
        return configAuditMapper.findByOperator(operator);
    }

    @Override
    public List<ConfigAudit> findByConditions(String tableName, Long recordId, String operator, int offset,
                                              int pageSize, String sortColumn, String sortOrder) {
        return configAuditMapper.findByConditions(tableName, recordId, operator, offset, pageSize, sortColumn, sortOrder);
    }

    @Override
    public long countByConditions(String tableName, Long recordId, String operator) {
        return configAuditMapper.countByConditions(tableName, recordId, operator);
    }
}
