package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.AuditQuery;
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
    public ConfigAudit save(ConfigAudit audit) {
        if (audit.getCreatedAt() == null) {
            audit.setCreatedAt(LocalDateTime.now());
        }
        configAuditMapper.insertConfigAudit(audit);
        return audit;
    }

    /**
     * 按表名与记录 ID 查询审计
     * 用于单条记录的变更追踪
     */
    @Override
    public List<ConfigAudit> findByTableNameAndRecordId(AuditQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return configAuditMapper.findByTableNameAndRecordId(query);
    }

    /**
     * 按操作人查询审计
     * 用于人员维度的变更分析
     */
    @Override
    public List<ConfigAudit> findByOperator(AuditQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return configAuditMapper.findByOperator(query);
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
}
