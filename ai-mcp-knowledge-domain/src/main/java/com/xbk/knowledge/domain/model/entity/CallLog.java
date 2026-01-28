package com.xbk.knowledge.domain.model.entity;

import com.xbk.knowledge.types.enums.CallStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 调用日志实体
 * 对应数据库表：ai_call_log
 *
 * @author xiexu
 */
@Entity
@Table(name = "ai_call_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallLog {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模型ID
     */
    @Column(name = "model_id", nullable = false)
    private Long modelId;

    /**
     * 任务类型
     */
    @Column(name = "task_type", length = 50)
    private String taskType;

    /**
     * 请求内容
     */
    @Column(name = "request_content", columnDefinition = "TEXT")
    private String requestContent;

    /**
     * 响应内容
     */
    @Column(name = "response_content", columnDefinition = "TEXT")
    private String responseContent;

    /**
     * 使用token数
     */
    @Column(name = "tokens_used", nullable = false)
    private Integer tokensUsed;

    /**
     * 响应时间（毫秒）
     */
    @Column(name = "response_time", nullable = false)
    private Long responseTime;

    /**
     * 状态（SUCCESS/FAILED/FALLBACK）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CallStatus status;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
