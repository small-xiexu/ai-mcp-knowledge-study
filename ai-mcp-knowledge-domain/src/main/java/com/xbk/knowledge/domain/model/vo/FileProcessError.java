package com.xbk.knowledge.domain.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件处理错误记录
 * 用于记录文件处理失败的详细信息
 *
 * 职责：值对象，封装文件处理错误信息
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessError {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 堆栈信息
     */
    private String stackTrace;

    /**
     * 发生时间
     */
    private LocalDateTime occurredAt;

    /**
     * 文件级重试次数（最多 3 次）
     */
    private int retryCount;
}
