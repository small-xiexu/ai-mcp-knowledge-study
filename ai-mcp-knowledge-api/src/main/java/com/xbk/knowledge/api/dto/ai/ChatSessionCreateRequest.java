package com.xbk.knowledge.api.dto.ai;

import lombok.Data;

import java.util.List;

/**
 * 会话创建请求
 *
 * @author sxie
 */
@Data
public class ChatSessionCreateRequest {

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
