package com.xbk.knowledge.orchestration.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模型能力实体
 * 对应数据库表：ai_model_capability
 *
 * @author xiexu
 */
@Entity
@Table(name = "ai_model_capability")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCapability {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模型ID（外键）
     */
    @Column(name = "model_id", nullable = false, unique = true)
    private Long modelId;

    /**
     * 最大输入token
     */
    @Column(name = "max_input_tokens", nullable = false)
    private Integer maxInputTokens;

    /**
     * 最大输出token
     */
    @Column(name = "max_output_tokens", nullable = false)
    private Integer maxOutputTokens;

    /**
     * 支持函数调用
     */
    @Column(name = "support_function_calling", nullable = false)
    private Boolean supportFunctionCalling;

    /**
     * 支持视觉
     */
    @Column(name = "support_vision", nullable = false)
    private Boolean supportVision;

    /**
     * 支持流式输出
     */
    @Column(name = "support_streaming", nullable = false)
    private Boolean supportStreaming;

    /**
     * 质量评分（1-100）
     */
    @Column(name = "quality_score", nullable = false)
    private Integer qualityScore;

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
     * 关联的模型配置（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", insertable = false, updatable = false)
    private ModelConfig modelConfig;

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
