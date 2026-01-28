package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 按条件分页查询审计日志
     * 支持表名、记录ID、操作人任意组合过滤
     *
     * @param tableName 表名
     * @param recordId  记录ID
     * @param operator  操作人
     * @param pageable  分页参数
     * @return 审计日志分页结果
     */
    @Query("""
            select audit
            from ConfigAudit audit
            where (:tableName is null or audit.tableName = :tableName)
              and (:recordId is null or audit.recordId = :recordId)
              and (:operator is null or audit.operator = :operator)
            """)
    Page<ConfigAudit> findByConditions(@Param("tableName") String tableName,
                                       @Param("recordId") Long recordId,
                                       @Param("operator") String operator,
                                       Pageable pageable);
}
