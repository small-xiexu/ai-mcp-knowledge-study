package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import org.apache.ibatis.annotations.Mapper;

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
     * 按条件分页查询审计日志
     *
     * @param query 审计查询条件
     * @return 审计日志列表
     */
    List<ConfigAudit> findByConditions(AuditQuery query);

    /**
     * 查询所有可用表名
     *
     * @return 表名列表
     */
    List<String> listTableNames();

    /**
     * 按条件统计审计日志数量
     *
     * @param query 审计查询条件
     * @return 总数
     */
    long countByConditions(AuditQuery query);
}
