package com.xbk.knowledge.domain.audit.adapter.repository;

import com.xbk.knowledge.domain.audit.model.aggregate.ConfigAuditAggregate;
import com.xbk.knowledge.domain.audit.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.audit.model.valobj.AuditQuery;

import java.util.List;

/**
 * 配置审计仓储接口
 * 通过仓储抽象隔离数据访问实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author sxie
 */
public interface ConfigAuditRepository {

    /**
     * 保存审计日志聚合
     *
     * 为什么：以聚合形式保存审计记录，保证一致性
     * 入参：审计日志聚合
     * 出参：保存后的聚合
     */
    ConfigAuditAggregate save(ConfigAuditAggregate aggregate);

    /**
     * 按条件分页查询审计日志
     *
     * 为什么：按筛选条件分页查询审计记录
     * 入参：审计查询条件
     * 出参：审计日志列表
     */
    List<ConfigAudit> findByConditions(AuditQuery query);

    /**
     * 按条件统计审计日志数量
     *
     * 为什么：分页展示需要总数
     * 入参：审计查询条件
     * 出参：总数
     */
    long countByConditions(AuditQuery query);

    /**
     * 查询所有可用表名
     *
     * 为什么：提供筛选下拉数据源
     * 入参：无
     * 出参：表名列表
     */
    List<String> listTableNames();
}
