package com.xbk.knowledge.api.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 话响应
 *
 * @author sxie
 */
@Data
public class ChatSessionResponse {

    /**
     * 会话 ID
     */
    private Long id;

    /**
     * 话标题
     */
    private String title;

    /**
     * 话默认模型ID
     */
    private Long modelId;

    /**
     * 关联知识库标签
     */
    private List<String> ragTags;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
