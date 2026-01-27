package com.xbk.knowledge.orchestration.domain.repository;

import com.xbk.knowledge.orchestration.domain.entity.ConfigAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 配置审计 Repository
 *
 * @author xiexu
 */
@Repository
public interface ConfigAuditRepository extends JpaRepository<ConfigAudit, Long> {

    /**
     * 根据表名和记录ID查询审计日志
     *
     * @param tableName 表名
     * @param recordId  记录ID
     * @return 审计日志列表
     */
    List<ConfigAudit> findByTableNameAndRecordId(String tableName, Long recordId);

    /**
     * 根据操作人查询审计日志
     *
     * @param operator 操作人
     * @return 审计日志列表
     */
    List<ConfigAudit> findByOperator(String operator);
}
