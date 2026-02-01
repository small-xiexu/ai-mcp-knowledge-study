package com.xbk.knowledge.api.dto.rag;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * RAG 任务ID请求
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author xiexu
 */
@Data
public class RagTaskIdRequest {

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;
}
