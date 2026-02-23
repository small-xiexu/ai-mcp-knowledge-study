package com.xbk.knowledge.api.dto.rag;

import com.xbk.knowledge.types.enums.RagTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RAG 任务响应 DTO
 *
 * 职责：接口层 DTO，用于承载响应参数并保证传输边界稳定
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagTaskResponse implements Serializable {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * task ID
     */
    private String taskId;
    /**
     * 类型
     */
    private String type;
    /**
     * 状态
     */
    private RagTaskStatus status;
    /**
     * progress
     */
    private Integer progress;
    /**
     * 消息
     */
    private String message;
    /**
     * RAGTag
     */
    private String ragTag;
    /**
     * 错误Details
     */
    private String errorDetails;
    /**
     * 重试数量
     */
    private Integer retryCount;
    /**
     * parentTask ID
     */
    private String parentTaskId;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
