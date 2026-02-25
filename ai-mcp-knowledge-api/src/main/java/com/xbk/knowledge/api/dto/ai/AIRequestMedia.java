package com.xbk.knowledge.api.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话媒体输入项
 *
 * 职责：接口层 DTO，用于承载图片与附件的输入数据
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequestMedia {

    /**
     * 媒体类型：image / attachment。
     */
    private String kind;

    /**
     * 原始文件名。
     */
    private String name;

    /**
     * MIME 类型。
     */
    private String mimeType;

    /**
     * 媒体数据（DataURL 或 Base64 文本）。
     */
    private String data;

    /**
     * 附件文本内容（可选）。
     */
    private String text;
}

