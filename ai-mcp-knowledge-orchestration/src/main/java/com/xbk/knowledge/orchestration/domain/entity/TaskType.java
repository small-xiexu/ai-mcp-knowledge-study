package com.xbk.knowledge.orchestration.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 任务类型实体
 * 对应数据库表：ai_task_type
 *
 * @author xiexu
 */
@Entity
@Table(name = "ai_task_type")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskType {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务名称
     */
    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    /**
     * 任务编码（唯一）
     */
    @Column(name = "task_code", nullable = false, unique = true, length = 50)
    private String taskCode;

    /**
     * 任务描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 首选模型ID
     */
    @Column(name = "preferred_model_id")
    private Long preferredModelId;

    /**
     * 备用模型ID列表（逗号分隔）
     */
    @Column(name = "fallback_model_ids", length = 500)
    private String fallbackModelIds;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建时自动设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 更新时自动设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
