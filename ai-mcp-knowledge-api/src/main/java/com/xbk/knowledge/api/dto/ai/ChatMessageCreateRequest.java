package com.xbk.knowledge.api.dto.ai;

import lombok.Data;

/**
 * 消息创建请求
 *
 * @author xiexu
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

    /**
     * 提示词 token 数
     */
    private Integer promptTokens;

    /**
     * 输出 token 数
     */
    private Integer completionTokens;

    /**
     * 总 token 数
     */
    private Integer totalTokens;
}
