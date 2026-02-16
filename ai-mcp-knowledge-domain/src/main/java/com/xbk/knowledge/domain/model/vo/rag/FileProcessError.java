package com.xbk.knowledge.domain.model.vo.rag;

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
     *
     * 为什么：标识失败的具体文件
     */
    private String fileName;

    /**
     * 错误信息
     *
     * 为什么：描述失败原因
     */
    private String errorMessage;

    /**
     * 堆栈信息
     *
     * 为什么：用于排查问题根因
     */
    private String stackTrace;

    /**
     * 发生时间
     *
     * 为什么：用于时序分析与排查
     */
    private LocalDateTime occurredAt;

    /**
     * 文件级重试次数（最多 3 次）
     *
     * 为什么：记录当前文件的重试状态
     */
    private int retryCount;
}
