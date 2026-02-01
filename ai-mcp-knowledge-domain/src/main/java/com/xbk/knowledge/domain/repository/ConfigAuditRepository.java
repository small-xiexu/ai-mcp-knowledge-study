package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.aggregate.audit.ConfigAuditAggregate;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;

import java.util.List;

/**
 * 配置审计仓储接口
 * 通过仓储抽象隔离数据访问实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author xiexu
 */
public interface ConfigAuditRepository {

    /**
     * 保存审计日志聚合
     *
     * @param aggregate 审计日志聚合
     * @return 保存后的聚合
     */
    ConfigAuditAggregate save(ConfigAuditAggregate aggregate);

    /**
     * 按条件分页查询审计日志
     *
     * @param query 审计查询条件
     * @return 审计日志列表
     */
    List<ConfigAudit> findByConditions(AuditQuery query);

    /**
     * 按条件统计审计日志数量
     *
     * @param query 审计查询条件
     * @return 总数
     */
    long countByConditions(AuditQuery query);

    /**
     * 查询所有可用表名
     *
     * @return 表名列表
     */
    List<String> listTableNames();
}
