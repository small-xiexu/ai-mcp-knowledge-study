package com.xbk.knowledge.api.dto.ai;

import lombok.Data;

/**
 * 消息创建请求
 *
 * @author sxie
 */
@Data
public class ChatMessageCreateRequest {

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

}
