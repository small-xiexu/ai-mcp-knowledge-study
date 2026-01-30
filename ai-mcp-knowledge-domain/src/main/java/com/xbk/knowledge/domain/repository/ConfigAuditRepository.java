package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.AuditQuery;

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
     * 保存审计日志
     *
     * @param audit 审计日志
     * @return 保存后的日志
     */
    ConfigAudit save(ConfigAudit audit);

    /**
     * 按表名和记录ID查询审计日志
     *
     * @param query 表名与记录ID查询条件
     * @return 审计日志列表
     */
    List<ConfigAudit> findByTableNameAndRecordId(AuditQuery query);

    /**
     * 按操作人查询审计日志
     *
     * @param query 操作人查询条件
     * @return 审计日志列表
     */
    List<ConfigAudit> findByOperator(AuditQuery query);

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
}
