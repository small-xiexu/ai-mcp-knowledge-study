package com.xbk.knowledge.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "role 不能为空")
    private String role;

    /**
     * 消息内容
     */
    @NotBlank(message = "content 不能为空")
    private String content;

    /**
     * 实际使用的模型ID
     */
    private Long modelId;

}
