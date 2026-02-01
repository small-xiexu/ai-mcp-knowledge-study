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
     * 为什么：落库审计记录
     * 入参：审计日志
     * 出参：影响行数
     */
    int insertConfigAudit(ConfigAudit audit);

    /**
     * 按条件分页查询审计日志
     *
     * 为什么：控制单次返回数量
     * 入参：审计查询条件
     * 出参：审计日志列表
     */
    List<ConfigAudit> findByConditions(AuditQuery query);

    /**
     * 查询所有可用表名
     *
     * 为什么：提供筛选下拉数据源
     * 入参：无
     * 出参：表名列表
     */
    List<String> listTableNames();

    /**
     * 按条件统计审计日志数量
     *
     * 为什么：分页展示需要总数
     * 入参：审计查询条件
     * 出参：总数
     */
    long countByConditions(AuditQuery query);
}
