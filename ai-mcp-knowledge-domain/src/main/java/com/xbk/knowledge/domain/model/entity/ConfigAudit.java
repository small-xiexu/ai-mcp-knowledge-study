package com.xbk.knowledge.domain.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 配置审计实体
 * 对应数据库表：ai_config_audit
 *
 * @author xiexu
 */
@Entity
@Table(name = "ai_config_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigAudit {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 表名
     */
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    /**
     * 记录ID
     */
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    /**
     * 操作类型（INSERT/UPDATE/DELETE）
     */
    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    /**
     * 旧值（JSON格式）
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * 新值（JSON格式）
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * 操作人
     */
    @Column(name = "operator", length = 100)
    private String operator;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 创建时自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
