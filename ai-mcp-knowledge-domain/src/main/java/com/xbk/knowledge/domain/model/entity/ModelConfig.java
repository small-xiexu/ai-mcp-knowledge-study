package com.xbk.knowledge.domain.model.entity;

import com.xbk.knowledge.types.enums.ModelType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模型配置实体
 * 对应数据库表：ai_model_config
 *
 * @author xiexu
 */
@Entity
@Table(name = "ai_model_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模型名称
     */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /**
     * 模型类型（OPENAI/ANTHROPIC/GEMINI）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 50)
    private ModelType modelType;

    /**
     * API密钥
     */
    @Column(name = "api_key", nullable = false, length = 500)
    private String apiKey;

    /**
     * API地址
     */
    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    /**
     * 是否启用（0:禁用 1:启用）
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * 优先级（数字越大优先级越高）
     */
    @Column(name = "priority", nullable = false)
    private Integer priority;

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
     * 模型能力（一对一关系）
     */
    @OneToOne(mappedBy = "modelConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ModelCapability capability;

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
