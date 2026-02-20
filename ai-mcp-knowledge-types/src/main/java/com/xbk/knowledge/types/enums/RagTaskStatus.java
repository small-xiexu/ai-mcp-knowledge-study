package com.xbk.knowledge.types.enums;

/**
 * RAG 任务状态枚举
 *
 * 职责：统一任务状态语义
 * @author sxie
 */
public enum RagTaskStatus {

    /**
     * 等待处理
     */
    PENDING,

    /**
     * 处理中
     */
    PROCESSING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 失败
     */
    FAILED,

    /**
     * 已取消
     */
    CANCELLED
}
