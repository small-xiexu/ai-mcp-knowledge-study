package com.xbk.knowledge.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 身份域审计事件实体。
 * 对应数据库表sys_audit_event
 *
 * 职责：领域实体，用于承载关键操作审计记录。
 *
 * @author sxie
 */
@TableName("sys_audit_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysAuditEventPO {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人ID。
     */
    private Long operatorId;

    /**
     * 操作人用户名（查询展示字段）。
     */
    private String operatorName;

    /**
     * 操作主体类型。
     */
    private String operatorType;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 资源ID。
     */
    private String resourceId;

    /**
     * 动作。
     */
    private String action;

    /**
     * 请求ID。
     */
    private String requestId;

    /**
     * 来源IP。
     */
    private String sourceIp;

    /**
     * User-Agent。
     */
    private String userAgent;

    /**
     * 旧值快照。
     */
    private String oldValue;

    /**
     * 新值快照。
     */
    private String newValue;

    /**
     * 结果1成功、0失败。
     */
    private Integer result;

    /**
     * 失败原因。
     */
    private String errorMessage;

    /**
     * 耗时毫秒。
     */
    private Long costMs;

    /**
     * 发生时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime occurredAt;
}
