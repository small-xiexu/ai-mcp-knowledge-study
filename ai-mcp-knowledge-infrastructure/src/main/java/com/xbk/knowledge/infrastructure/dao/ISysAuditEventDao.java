package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.SysAuditEventPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.identity.model.valobj.AuditEventPageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 身份域审计事件 Mapper。
 *
 * 职责：MyBatis Mapper 接口，用于写入 sys_audit_event。
 *
 * @author sxie
 */
@Mapper
public interface ISysAuditEventDao extends BaseMapper<SysAuditEventPO> {

    /**
     * 插入审计事件。
     *
     * @param event 审计事件
     * @return 影响行数
     */
    int insertEvent(SysAuditEvent event);

    /**
     * 分页查询审计事件。
     *
     * @param query 查询条件
     * @return 列表
     */
    List<SysAuditEvent> findPage(AuditEventPageQuery query);

    /**
     * 统计审计事件数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    long count(AuditEventPageQuery query);
}
