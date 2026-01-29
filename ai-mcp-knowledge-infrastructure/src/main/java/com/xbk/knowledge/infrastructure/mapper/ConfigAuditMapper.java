package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 配置审计 Mapper
 * 统一通过 XML 承载 SQL
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 * @author xiexu
 */
@Mapper
public interface ConfigAuditMapper extends BaseMapper<ConfigAudit> {

    /**
     * 新增审计日志
     *
     * @param audit 审计日志
     * @return 影响行数
     */
    int insertConfigAudit(ConfigAudit audit);

    /**
     * 按表名和记录ID查询审计日志
     *
     * @param tableName 表名
     * @param recordId  记录ID
     * @return 审计日志列表
     */
    List<ConfigAudit> findByTableNameAndRecordId(@Param("tableName") String tableName,
                                                 @Param("recordId") Long recordId);

    /**
     * 按操作人查询审计日志
     *
     * @param operator 操作人
     * @return 审计日志列表
     */
    List<ConfigAudit> findByOperator(@Param("operator") String operator);

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
    List<ConfigAudit> findByConditions(@Param("tableName") String tableName,
                                       @Param("recordId") Long recordId,
                                       @Param("operator") String operator,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize,
                                       @Param("sortColumn") String sortColumn,
                                       @Param("sortOrder") String sortOrder);

    /**
     * 按条件统计审计日志数量
     *
     * @param tableName 表名
     * @param recordId  记录ID
     * @param operator  操作人
     * @return 总数
     */
    long countByConditions(@Param("tableName") String tableName,
                           @Param("recordId") Long recordId,
                           @Param("operator") String operator);
}
