package com.xbk.knowledge.api.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息响应
 *
 * @author sxie
 */
@Data
public class ChatMessageResponse {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 消息角色(user/assistant)
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 实际使用的模型ID
     */
    private Long modelId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
