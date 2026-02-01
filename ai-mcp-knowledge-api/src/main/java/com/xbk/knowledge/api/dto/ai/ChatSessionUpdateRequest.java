package com.xbk.knowledge.api.dto.ai;

import lombok.Data;

import java.util.List;

/**
 * 会话更新请求
 *
 * @author xiexu
 */
@Data
public class ChatSessionUpdateRequest {

    /**
     * 会话标题
     */
    private String title;

    /**
     * 会话默认模型ID
     */
    private Long modelId;

    /**
     * 关联知识库标签
     */
    private List<String> ragTags;
}
