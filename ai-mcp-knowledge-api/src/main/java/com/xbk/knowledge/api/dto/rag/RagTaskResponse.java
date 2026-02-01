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
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagTaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private String type;
    private RagTaskStatus status;
    private Integer progress;
    private String message;
    private String ragTag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
