package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;

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
     * @param tableName 表名
     * @param recordId  记录ID
     * @return 审计日志列表
     */
    List<ConfigAudit> findByTableNameAndRecordId(String tableName, Long recordId);

    /**
     * 按操作人查询审计日志
     *
     * @param operator 操作人
     * @return 审计日志列表
     */
    List<ConfigAudit> findByOperator(String operator);

    /**
     * 按条件分页查询审计日志
     *
     * @param tableName  表名
     * @param recordId   记录ID
     * @param operator   操作人
     * @param offset     偏移量
     * @param pageSize   每页大小
     * @param sortColumn 排序字段
     * @param sortOrder  排序方向
     * @return 审计日志列表
     */
    List<ConfigAudit> findByConditions(String tableName, Long recordId, String operator,
                                       int offset, int pageSize, String sortColumn, String sortOrder);

    /**
     * 按条件统计审计日志数量
     *
     * @param tableName 表名
     * @param recordId  记录ID
     * @param operator  操作人
     * @return 总数
     */
    long countByConditions(String tableName, Long recordId, String operator);
}
