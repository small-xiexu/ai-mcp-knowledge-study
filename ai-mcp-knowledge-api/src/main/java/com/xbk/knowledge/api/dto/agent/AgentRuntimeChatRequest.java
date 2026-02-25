package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Agent 运行入口请求（同步 chat）。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentRuntimeChatRequest extends BaseRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 可选会话 ID（用于未来绑定话/记忆/RAG）。
     */
    private Long sessionId;

    /**
     * 用户输入。
     */
    @NotBlank(message = "content 不能为空")
    private String content;

    /**
     * 可选RAG 标签 JSON（数组字符串）。
     * P0 先透传保存，后续按 AgentVersion.allowedRagTagsJson 做治理。
     */
    private String ragTagsJson;
}

